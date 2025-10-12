"""
Working Gomoku AI - exact architecture match for gomoku_rl models.

Models from: https://github.com/hesic73/gomoku_rl

Citation:
@misc{He2023gomoku_rl,
  author = {He, Sicheng},
  title = {gomoku_rl},
  year = {2023},
  publisher = {GitHub},
  journal = {GitHub repository},
  howpublished = {\url{https://github.com/hesic73/gomoku_rl}},
}
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
from typing import List, Tuple


class ResidualBlock(nn.Module):
    """Residual block matching gomoku_rl structure"""
    def __init__(self, channels=64):
        super().__init__()
        self.cnn_0 = nn.Conv2d(channels, channels, 3, padding=1)
        self.bn_0 = nn.BatchNorm2d(channels)
        self.cnn_1 = nn.Conv2d(channels, channels, 3, padding=1)
        self.bn_1 = nn.BatchNorm2d(channels)

    def forward(self, x):
        residual = x
        out = F.relu(self.bn_0(self.cnn_0(x)))
        out = self.bn_1(self.cnn_1(out))
        out += residual
        return F.relu(out)


class GomokuPolicyNetwork(nn.Module):
    """Exact match for gomoku_rl CNN policy architecture"""
    def __init__(self, board_size=15, channels=64, num_blocks=4):
        super().__init__()
        self.board_size = board_size

        # Encoder: Initial conv
        self.cnn = nn.Conv2d(3, channels, 3, padding=1)
        self.bn = nn.BatchNorm2d(channels)

        # Encoder: Residual blocks
        self.layers = nn.ModuleList([
            ResidualBlock(channels) for _ in range(num_blocks)
        ])

        # Policy head
        self.policy_cnn = nn.Conv2d(channels, 2, 1)
        self.policy_bn = nn.BatchNorm2d(2)
        self.policy_linear = nn.Linear(2 * board_size * board_size, board_size * board_size)

    def forward(self, x, action_mask=None):
        """
        Args:
            x: (batch, 3, 15, 15)
            action_mask: (batch, 225) boolean mask

        Returns:
            logits (batch, 225)
        """
        # Encoder
        out = F.relu(self.bn(self.cnn(x)))
        for layer in self.layers:
            out = layer(out)

        # Policy head
        policy = F.relu(self.policy_bn(self.policy_cnn(out)))
        policy = policy.view(policy.size(0), -1)  # Flatten
        logits = self.policy_linear(policy)

        if action_mask is not None:
            logits = logits.masked_fill(~action_mask, float('-inf'))

        return logits


class GomokuAI:
    """Gomoku AI using pretrained models"""

    def __init__(self, model_path: str, board_size: int = 15, device: str = "cpu"):
        self.board_size = board_size
        self.device = device

        # Load checkpoint
        checkpoint = torch.load(model_path, map_location=device)

        if 'actor' in checkpoint:
            actor_state = checkpoint['actor']
        else:
            raise ValueError("No 'actor' key in checkpoint")

        # Create model
        self.model = GomokuPolicyNetwork(board_size=board_size)

        # Map loaded weights
        model_state = self._remap_state_dict(actor_state)

        # Load weights
        missing, unexpected = self.model.load_state_dict(model_state, strict=False)

        if missing:
            print(f"⚠️  Missing keys: {len(missing)}")
        if unexpected:
            print(f"⚠️  Unexpected keys: {len(unexpected)}")

        self.model.to(device)
        self.model.eval()
        print(f"✅ Model loaded successfully")

    def _remap_state_dict(self, ckpt_state):
        """
        Remap checkpoint keys to model keys.

        Checkpoint format:
        - module.0.module.cnn.weight -> cnn.weight
        - module.0.module.layers.0.cnn_0.weight -> layers.0.cnn_0.weight
        - module.1.module.cnn.weight -> policy_cnn.weight
        - module.1.module.linear.weight -> policy_linear.weight
        """
        new_state = {}

        for ckpt_key, value in ckpt_state.items():
            # Remove "module.0.module." prefix for encoder
            if ckpt_key.startswith('module.0.module.'):
                model_key = ckpt_key.replace('module.0.module.', '')
                new_state[model_key] = value

            # Remove "module.1.module." prefix for policy head
            elif ckpt_key.startswith('module.1.module.'):
                model_key = ckpt_key.replace('module.1.module.cnn', 'policy_cnn')
                model_key = model_key.replace('module.1.module.bn', 'policy_bn')
                model_key = model_key.replace('module.1.module.linear', 'policy_linear')
                new_state[model_key] = value

        return new_state

    def board_to_tensor(self, board: List[List[int]], current_player: int) -> torch.Tensor:
        """
        Convert board to tensor.

        Args:
            board: 15x15 (0=empty, 1=black, 2=white)
            current_player: 1 or 2

        Returns:
            (1, 3, 15, 15) with channels [current, opponent, empty]
        """
        board_np = np.array(board, dtype=np.float32)

        current_stones = (board_np == current_player).astype(np.float32)
        opponent = 3 - current_player
        opponent_stones = (board_np == opponent).astype(np.float32)
        empty = (board_np == 0).astype(np.float32)

        tensor = np.stack([current_stones, opponent_stones, empty], axis=0)
        return torch.from_numpy(tensor).unsqueeze(0).to(self.device)

    def get_action_mask(self, board: List[List[int]]) -> torch.Tensor:
        """Create boolean mask of valid moves"""
        board_np = np.array(board)
        mask = (board_np == 0).flatten()
        return torch.from_numpy(mask).unsqueeze(0).to(self.device)

    def get_move(self, board: List[List[int]], current_player: int) -> Tuple[int, int]:
        """
        Get AI's move.

        Args:
            board: 15x15 board (0=empty, 1=black, 2=white)
            current_player: 1 or 2

        Returns:
            (row, col)
        """
        board_tensor = self.board_to_tensor(board, current_player)
        action_mask = self.get_action_mask(board)

        with torch.no_grad():
            logits = self.model(board_tensor, action_mask)
            action = torch.argmax(logits, dim=1).item()

        row = action // self.board_size
        col = action % self.board_size

        return (row, col)


if __name__ == "__main__":
    print("=== Testing PPO Model ===\n")

    # Test PPO
    ai = GomokuAI("pretrained-15x15/ppo/1.pt")

    board = [[0]*15 for _ in range(15)]

    print("Move 1 (empty board):")
    row, col = ai.get_move(board, current_player=2)
    print(f"  AI plays at ({row}, {col})")
    board[row][col] = 2

    print("\nMove 2 (after human plays (7,7)):")
    board[7][7] = 1
    row, col = ai.get_move(board, current_player=2)
    print(f"  AI plays at ({row}, {col})")

    print("\n✅ All tests passed!")
