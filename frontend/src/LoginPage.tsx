import {
    signInWithPopup, 
    GoogleAuthProvider,
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword, 
    type User
} from 'firebase/auth';
import { useState } from 'react';
import { auth } from './firebase';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleSyncProfile = async (token: string) => {
        try {
            const response = await fetch('http://localhost:8080/api/profiles/sync', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                throw new Error('Failed to sync profile');
            }
            console.log('Profile synced successfully');
        } catch (err) {
            if (err instanceof Error) {
                setError('Could not sync your profile with the server.');
            }
            console.error('Profile sync error:', err);
        }
    };

    const handleFirstLogin = async (user: User) => {
        const token = await user.getIdToken();
        const isNewUser = user.metadata.creationTime === user.metadata.lastSignInTime;
        if (isNewUser) {
            await handleSyncProfile(token);
        }
    };

    const handleGoogleSignIn = async () => {
        const provider = new GoogleAuthProvider();
        try {
            setError('');
            const result = await signInWithPopup(auth, provider);
            await handleFirstLogin(result.user);
        } catch (error: unknown) {
            if (error instanceof Error) {
                setError(error.message);
            }
            console.error("Google sign-in error: ", error);
        }
    };

    const handleEmailSignUp = async () => {
        try {
            setError('');
            if(password.length < 6) {
                setError("Password must be at least 6 characters long.");
                return;
            }
            const userCredential = await createUserWithEmailAndPassword(auth, email, password);
            await handleFirstLogin(userCredential.user);
        } catch (error: unknown) {
            if (error instanceof Error) {
                setError(error.message);
            }
        }
    };

    const handleEmailSignIn = async () => {
        try {
            setError('');
            await signInWithEmailAndPassword(auth, email, password);
        } catch (error: unknown) {
            if (error instanceof Error) {
                setError(error.message);
            }
        }
    };

    return (
        <div className="w-full max-w-xs p-8 space-y-6 bg-gray-800 rounded-lg shadow-md">
            <h2 className="text-2xl font-bold text-center">Login or Sign Up</h2>
            
            {error && <p className="text-red-500 text-sm text-center">{error}</p>}

            <div className="space-y-4">
                <input 
                    type="email" 
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Email"
                    className="w-full px-4 py-2 text-gray-900 bg-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <input 
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Password"
                    className="w-full px-4 py-2 text-gray-900 bg-gray-200 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
            </div>

            <div className="flex flex-col space-y-4">
                <button onClick={handleEmailSignIn} className="w-full py-2 font-bold text-white bg-blue-600 rounded-md hover:bg-blue-700">Sign In</button>
                <button onClick={handleEmailSignUp} className="w-full py-2 font-bold text-white bg-green-600 rounded-md hover:bg-green-700">Sign Up with Email</button>
            </div>

            <div className="relative flex items-center justify-center my-4">
                <div className="flex-grow border-t border-gray-600"></div>
                <span className="flex-shrink mx-4 text-gray-400">Or</span>
                <div className="flex-grow border-t border-gray-600"></div>
            </div>

            <button onClick={handleGoogleSignIn} className="w-full flex items-center justify-center py-2 font-bold text-white bg-red-600 rounded-md hover:bg-red-700">
                <svg className="w-5 h-5 mr-2" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48px" height="48px"><path fill="#fbc02d" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12 s5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24s8.955,20,20,20 s20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"/><path fill="#e53935" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657 C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"/><path fill="#4caf50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36 c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"/><path fill="#1565c0" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.574l6.19,5.238 C42.022,35.244,44,30.036,44,24C44,22.659,43.862,21.35,43.611,20.083z"/></svg>
                Sign In with Google
            </button>
        </div>
    );
};

export default LoginPage;
