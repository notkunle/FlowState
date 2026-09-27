import { SessionProvider, useSession } from "./context/SessionContext";
import SessionControls from "./components/SessionControls";
import StateBadge from "./components/StateBadge";
import VitalsReadout from "./components/VitalsReadout";
import FocusChart from "./components/FocusChart";
import { useVitalsStream } from "./hooks/useVitalsStream";
import { colors } from "./styles/colors";

function Dashboard() {
    const { session } = useSession();
    const { vitals, baseline, decision } = useVitalsStream(session?.id);

    return (
        <div style={{ background: colors.surface1, minHeight: "100vh", padding: "1.5rem" }}>
            <SessionControls />

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", margin: "1.5rem 0 12px" }}>
                <StateBadge decision={decision} />
                <VitalsReadout vitals={vitals} baseline={baseline} />
            </div>

            <FocusChart />
        </div>
    );
}

export default function App() {
    return (
        <SessionProvider>
            <Dashboard />
        </SessionProvider>
    );
}
