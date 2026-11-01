import { Box, ToggleButton, Typography } from '@mui/material'
import { DAYS, MODULES } from '../utils/labels'

// Clickable grid (module x day). value: [{ day, module }]
export default function ScheduleGrid({ value, onChange }) {
  const isSelected = (day, module) => value.some((b) => b.day === day && b.module === module)

  const toggle = (day, module) => {
    onChange(isSelected(day, module)
        ? value.filter((b) => !(b.day === day && b.module === module))
        : [...value, { day, module }])
  }

  return (
      <Box sx={{ display: 'grid', gridTemplateColumns: `110px repeat(${DAYS.length}, 1fr)`, gap: 0.5, alignItems: 'center' }}>
        <span />
        {DAYS.map((d) => <Typography key={d} variant="caption" align="center" sx={{ fontWeight: 600 }}>{d}</Typography>)}
        {Object.entries(MODULES).map(([module, hours]) => (
            <Box key={module} sx={{ display: 'contents' }}>
              <Typography variant="caption">M{module} {hours}</Typography>
              {DAYS.map((d) => (
                  <ToggleButton
                      key={d + module}
                      size="small"
                      value={d + module}
                      selected={isSelected(d, Number(module))}
                      onChange={() => toggle(d, Number(module))}
                      sx={{ py: 0.2 }}
                  >
                    {d}{module}
                  </ToggleButton>
              ))}
            </Box>
        ))}
      </Box>
  )
}