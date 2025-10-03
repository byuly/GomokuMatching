"""
Neural network architecture for Gomoku AI.

Inspired by AlphaGo Zero, uses a convolutional neural network with:
- Multiple residual blocks for feature extraction
- Dual heads: policy (move probabilities) and value (position evaluation)
"""
import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Tuple


class ResidualBlock(nn.Module):
    """
    Residual block with two convolutional layers and skip connection.

    Architecture:
    Input -> Conv -> BatchNorm -> ReLU -> Conv -> BatchNorm -> Add(Input) -> ReLU
    """

    def __init__(self, channels: int):
        """
        Args:
            channels: Number of input/output channels
        """
        super(ResidualBlock, self).__init__()
        self.conv1 = nn.Conv2d(channels, channels, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm2d(channels)
        self.conv2 = nn.Conv2d(channels, channels, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm2d(channels)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Forward pass with residual connection.

        Args:
            x: Input tensor (batch_size, channels, height, width)

        Returns:
            Output tensor (same shape as input)
        """
        residual = x
        out = F.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))
        out += residual
        out = F.relu(out)
        return out


class GomokuNet(nn.Module):
    """
    Neural network for Gomoku AI with policy and value heads.

    Architecture:
    - Input layer: Converts board state to feature planes
    - Body: Multiple residual blocks for feature extraction
    - Policy head: Outputs move probabilities for each position
    - Value head: Outputs position evaluation (-1 to 1)

    Input shape: (batch_size, 3, 15, 15)
        - Channel 0: Current player's stones
        - Channel 1: Opponent's stones
        - Channel 2: Current player indicator (all 0s or all 1s)

    Output:
        - Policy: (batch_size, 225) - probabilities for all 15x15 positions
        - Value: (batch_size, 1) - position evaluation
    """

    def __init__(self, num_residual_blocks: int = 5, num_channels: int = 128):
        """
        Initialize network.

        Args:
            num_residual_blocks: Number of residual blocks (default 5 for balance)
            num_channels: Number of feature channels (default 128)
        """
        super(GomokuNet, self).__init__()

        self.num_channels = num_channels
        self.board_size = 15

        # Input convolution: 3 input channels -> num_channels
        self.input_conv = nn.Conv2d(3, num_channels, kernel_size=3, padding=1)
        self.input_bn = nn.BatchNorm2d(num_channels)

        # Residual tower
        self.residual_blocks = nn.ModuleList([
            ResidualBlock(num_channels) for _ in range(num_residual_blocks)
        ])

        # Policy head: predicts move probabilities
        self.policy_conv = nn.Conv2d(num_channels, 32, kernel_size=1)
        self.policy_bn = nn.BatchNorm2d(32)
        self.policy_fc = nn.Linear(32 * self.board_size * self.board_size,
                                   self.board_size * self.board_size)

        # Value head: evaluates position
        self.value_conv = nn.Conv2d(num_channels, 16, kernel_size=1)
        self.value_bn = nn.BatchNorm2d(16)
        self.value_fc1 = nn.Linear(16 * self.board_size * self.board_size, 256)
        self.value_fc2 = nn.Linear(256, 1)

    def forward(self, x: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Forward pass through the network.

        Args:
            x: Input tensor (batch_size, 3, 15, 15)

        Returns:
            Tuple of (policy, value):
                - policy: (batch_size, 225) - move probabilities (after softmax)
                - value: (batch_size, 1) - position value in [-1, 1] (after tanh)
        """
        # Input processing
        x = F.relu(self.input_bn(self.input_conv(x)))

        # Residual tower
        for block in self.residual_blocks:
            x = block(x)

        # Policy head
        policy = F.relu(self.policy_bn(self.policy_conv(x)))
        policy = policy.view(-1, 32 * self.board_size * self.board_size)
        policy = self.policy_fc(policy)
        policy = F.softmax(policy, dim=1)

        # Value head
        value = F.relu(self.value_bn(self.value_conv(x)))
        value = value.view(-1, 16 * self.board_size * self.board_size)
        value = F.relu(self.value_fc1(value))
        value = torch.tanh(self.value_fc2(value))

        return policy, value

    def predict(self, board_state: torch.Tensor) -> Tuple[torch.Tensor, float]:
        """
        Make a prediction for a single board state.

        Args:
            board_state: Tensor (3, 15, 15) representing the board

        Returns:
            Tuple of (policy, value):
                - policy: Tensor (225,) of move probabilities
                - value: Float in [-1, 1] for position evaluation
        """
        self.eval()
        with torch.no_grad():
            # Add batch dimension
            x = board_state.unsqueeze(0)
            policy, value = self.forward(x)
            return policy.squeeze(0), value.item()


def create_model(device: str = 'cpu', num_residual_blocks: int = 5) -> GomokuNet:
    """
    Factory function to create and initialize a GomokuNet model.

    Args:
        device: 'cpu' or 'cuda'
        num_residual_blocks: Number of residual blocks

    Returns:
        Initialized GomokuNet model on specified device
    """
    model = GomokuNet(num_residual_blocks=num_residual_blocks)
    model = model.to(device)
    return model


def board_to_tensor(board: list, current_player: int) -> torch.Tensor:
    """
    Convert board state to neural network input tensor.

    Args:
        board: 15x15 list of integers (0=empty, 1=black, 2=white)
        current_player: 1 for black, 2 for white

    Returns:
        Tensor (3, 15, 15):
            - Channel 0: Current player's stones (1s where player has stones)
            - Channel 1: Opponent's stones (1s where opponent has stones)
            - Channel 2: Current player indicator (all 0s for black, all 1s for white)
    """
    import numpy as np

    board_array = np.array(board, dtype=np.float32)
    opponent = 2 if current_player == 1 else 1

    # Create 3 feature planes
    current_plane = (board_array == current_player).astype(np.float32)
    opponent_plane = (board_array == opponent).astype(np.float32)
    player_plane = np.full((15, 15), current_player - 1, dtype=np.float32)  # 0 for black, 1 for white

    # Stack into 3-channel tensor
    tensor = np.stack([current_plane, opponent_plane, player_plane], axis=0)
    return torch.from_numpy(tensor)
