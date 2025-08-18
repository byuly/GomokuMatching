import { useEffect, useState } from 'react';
import { type User, onAuthStateChanged, signOut } from 'firebase/auth';
import { auth } from './firebase'; // Assumes firebase.ts is created
import LoginPage from './LoginPage';
import './index.css'

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Listen for authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });

    // Cleanup subscription on unmount
    return () => unsubscribe();
  }, []);

  const handleLogout = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      console.error("Error signing out: ", error);
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center h-screen">Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white flex flex-col items-center justify-center">
      {user ? (
        // Logged-in User View
        <div className="text-center">
          <h1 className="text-4xl font-bold mb-4">Welcome to Gomoku Online</h1>
          <p className="mb-6">Hello, {user.displayName || user.email}!</p>
          <button 
            onClick={handleLogout} 
            className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
          >
            Logout
          </button>
        </div>
      ) : (
        // Landing Page / Login View
        <div className="text-center">
            <h1 className="text-5xl font-bold mb-4">Gomoku Online</h1>
            <p className="text-xl text-gray-400 mb-8">The classic strategy game, reimagined.</p>
            <LoginPage />
        </div>
      )}
    </div>
  );
}

export default App;