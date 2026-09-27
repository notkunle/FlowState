import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// port 5173 is already in the backend's CORS allow-list
export default defineConfig({
    plugins: [react()],
    server: { port: 5173, strictPort: true },
});
