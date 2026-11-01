import {
    Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
    Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from '@mui/material'
import { statusColor } from '../utils/labels'

// Page title with an optional subtitle; children are the page actions (buttons, filters).
export function PageHeader({ title, subtitle, children }) {
    return (
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 2, mb: 3 }}>
            <Box>
                <Typography variant="h5">{title}</Typography>
                {subtitle && <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>{subtitle}</Typography>}
            </Box>
            <Stack direction="row" sx={{ gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>{children}</Stack>
        </Stack>
    )
}

// Titled card for grouping information.
export function SectionCard({ title, children }) {
    return (
        <Card sx={{ mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
                {title && <Typography variant="h6" sx={{ mb: 2 }}>{title}</Typography>}
                {children}
            </CardContent>
        </Card>
    )
}

// Small label above a value.
export function InfoItem({ label, children }) {
    return (
        <Box>
            <Typography variant="caption" sx={{ color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5 }}>{label}</Typography>
            <Typography component="div" sx={{ fontWeight: 500 }}>{children}</Typography>
        </Box>
    )
}

export function StatusChip({ value, labels }) {
    return <Chip size="small" label={labels?.[value] || value} color={statusColor(value)} />
}

// columns: [{ label, render: (row) => node }]
export function SimpleTable({ columns, rows, loading, emptyText = 'Sin registros' }) {
    return (
        <TableContainer component={Paper} variant="outlined" sx={{ borderColor: '#e1e7ee' }}>
            <Table size="small">
                <TableHead>
                    <TableRow>
                        {columns.map((c) => (
                            <TableCell key={c.label} sx={{ fontWeight: 700 }}>{c.label}</TableCell>
                        ))}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {loading && (
                        <TableRow>
                            <TableCell colSpan={columns.length} align="center"><CircularProgress size={22} /></TableCell>
                        </TableRow>
                    )}
                    {!loading && rows.length === 0 && (
                        <TableRow>
                            <TableCell colSpan={columns.length} align="center" sx={{ color: 'text.secondary', py: 3 }}>{emptyText}</TableCell>
                        </TableRow>
                    )}
                    {!loading && rows.map((row, i) => (
                        <TableRow key={row.id ?? i} hover>
                            {columns.map((c) => (
                                <TableCell key={c.label}>{c.render(row)}</TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    )
}

export function ConfirmDialog({ open, title, text, onCancel, onConfirm }) {
    return (
        <Dialog open={open} onClose={onCancel}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent><Typography>{text}</Typography></DialogContent>
            <DialogActions>
                <Button onClick={onCancel}>Cancelar</Button>
                <Button color="error" variant="contained" onClick={onConfirm}>Confirmar</Button>
            </DialogActions>
        </Dialog>
    )
}

export function Empty({ children }) {
    return <Box sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}>{children}</Box>
}

// Compact metric card: tinted icon, label, big value and an optional caption.
export function StatCard({ icon, label, value, caption }) {
    return (
        <Card>
            <CardContent sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                <Box sx={{ width: 48, height: 48, borderRadius: 2, bgcolor: 'rgba(15,118,110,0.1)', color: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                    {icon}
                </Box>
                <Box sx={{ minWidth: 0 }}>
                    <Typography variant="caption" sx={{ color: 'text.secondary', textTransform: 'uppercase', letterSpacing: 0.5 }}>{label}</Typography>
                    <Typography variant="h5" sx={{ lineHeight: 1.2 }}>{value}</Typography>
                    {caption && <Typography variant="body2" sx={{ color: 'text.secondary' }}>{caption}</Typography>}
                </Box>
            </CardContent>
        </Card>
    )
}