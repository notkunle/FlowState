import { useSession } from "./context/SessionContext";
import { useVitalsStream } from "./hooks/useVitalsStream";
import SessionControls from "./components/SessionControls";
import StateBadge from "./components/StateBadge";
import VitalsReadout from "./components/VitalsReadout";
import FocusChart from "./components/FocusChart";
import { colors } from "./styles/colors";

export default function App() {
    const { session } = useSession();
    const { vitals, baseline, decision, connected } = useVitalsStream(session?.id);

    return (
        <main className="app">
            <header>
                <h1>Flow State</h1>
                <p style={{ color: colors.textSecondary }}>
                    Focus coaching from your own vitals, compared with your own baseline.
                </p>
            </header>

            <section className="card">
                <SessionControls />
                {session && (
                    <p style={{ fontSize: "12px", color: colors.textMuted, margin: "6px 0 0" }}>
                        {connected ? "Live" : "Connecting..."}
                        {!decision && " · first check-in comes after a ~3 min baseline"}
                    </p>
                )}
            </section>

            {decision?.state === "stress" && (
                <section className="card break-card">
                    <strong>Time for a short break</strong>
                    <p>{decision.suggestion}</p>
                </section>
            )}

            <StateBadge decision={session ? decision : null} />
            <VitalsReadout vitals={vitals} baseline={baseline} />

            <section className="card">
                <FocusChart />
            </section>
        </main>
    );
}
