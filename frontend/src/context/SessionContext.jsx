import { createContext, useContext, useEffect, useState } from "react";
import { getActiveSession } from "../api/sessionApi";

const SessionContext = createContext(undefined);

export function SessionProvider({ children }) {
    const [session, setSession] = useState(null);

    // On load, re-attach to a session that is already running.
    //
    // Without this the UI can deadlock: the backend has an active session, so
    // POST /api/sessions returns 409 "A session is already running", but the UI
    // never learned the session id, so it never opens the vitals stream and
    // shows "No active session" while readings flow into the database behind
    // it. That happens after any page refresh, or whenever the backend was
    // started before the browser was opened.
    useEffect(() => {
        let cancelled = false;
        getActiveSession()
            .then((active) => {
                if (!cancelled && active) {
                    setSession(active);
                }
            })
            .catch((err) => {
                // Backend not up yet is not worth surfacing — the Start button
                // will report it properly when the user actually clicks it.
                console.warn("Could not check for an active session:", err);
            });
        return () => {
            cancelled = true;
        };
    }, []);

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
