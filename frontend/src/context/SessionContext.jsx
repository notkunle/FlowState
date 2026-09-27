import { createContext, useContext, useState } from "react";

const SessionContext = createContext(undefined);

export function SessionProvider({ children }) {
    const [session, setSession] = useState(null);

    return (
        <SessionContext.Provider value={{ session, setSession }}>
            {children}
        </SessionContext.Provider>
    );
}

export function useSession() {
    const context = useContext(SessionContext);
    if (context === undefined) {
        throw new Error("useSession must be used within a SessionProvider");
    }
    return context;
}
