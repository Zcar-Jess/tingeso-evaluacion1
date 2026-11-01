import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import './index.css'
import App from './App.jsx'

// SIGA IPT visual identity: deep navy sidebar, teal as the main action color, soft light surfaces.
const theme = createTheme({
    palette: {
        primary: { main: '#0f766e' },
        secondary: { main: '#0b1f3a' },
        background: { default: '#f3f6f9' },
        text: { primary: '#1b2a3a', secondary: '#5b6b7c' },
    },
    typography: {
        fontFamily: '"Inter", "Segoe UI", "Helvetica Neue", Arial, sans-serif',
        h5: { fontWeight: 700, fontSize: '1.6rem', color: '#0b1f3a' },
        h6: { fontWeight: 600, fontSize: '1.1rem', color: '#0b1f3a' },
    },
    shape: { borderRadius: 10 },
    components: {
        MuiCard: { defaultProps: { variant: 'outlined' }, styleOverrides: { root: { borderColor: '#e1e7ee' } } },
        MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
        MuiTableCell: {
            styleOverrides: {
                head: { backgroundColor: '#f3f6f9', color: '#4b5d70', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.04em' },
            },
        },
    },
})

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <App />
        </ThemeProvider>
    </StrictMode>,
)