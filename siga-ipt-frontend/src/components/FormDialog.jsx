import { useEffect, useState } from 'react'
import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel,
  MenuItem, Stack, TextField,
} from '@mui/material'
import { errorMessage } from '../api/http-common'
import { useNotify } from './Notify'

/**
 * Generic form dialog.
 * fields: [{ name, label, type: 'text'|'number'|'select'|'multiselect'|'checkbox', options, required, disabled }]
 * onSubmit(values) must return a promise; the dialog closes when it resolves.
 */
export default function FormDialog({ open, title, fields, initialValues = {}, submitLabel = 'Guardar', onClose, onSubmit }) {
  const notify = useNotify()
  const [values, setValues] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) setValues(initialValues)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const set = (name, value) => setValues((v) => ({ ...v, [name]: value }))

  const handleSubmit = async () => {
    const missing = fields.find((f) => f.required && (values[f.name] === undefined || values[f.name] === '' || values[f.name] === null))
    if (missing) {
      notify(`El campo "${missing.label}" es obligatorio`, 'warning')
      return
    }
    const payload = { ...values }
    fields.filter((f) => f.type === 'number').forEach((f) => {
      if (payload[f.name] !== '' && payload[f.name] != null) payload[f.name] = Number(payload[f.name])
    })
    setSaving(true)
    try {
      await onSubmit(payload)
      onClose()
    } catch (error) {
      notify(errorMessage(error), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
      <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {fields.map((f) => {
              if (f.type === 'checkbox') {
                return (
                    <FormControlLabel
                        key={f.name}
                        label={f.label}
                        control={<Checkbox checked={!!values[f.name]} onChange={(e) => set(f.name, e.target.checked)} />}
                    />
                )
              }
              const isSelect = f.type === 'select' || f.type === 'multiselect'
              const multiple = f.type === 'multiselect'
              return (
                  <TextField
                      key={f.name}
                      label={f.label}
                      type={f.type === 'number' ? 'number' : 'text'}
                      select={isSelect}
                      required={f.required}
                      disabled={f.disabled}
                      size="small"
                      fullWidth
                      value={values[f.name] ?? (multiple ? [] : '')}
                      onChange={(e) => set(f.name, e.target.value)}
                      slotProps={{ select: { multiple } }}
                  >
                    {isSelect && (f.options || []).map((o) => (
                        <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                    ))}
                  </TextField>
              )
            })}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={saving}>{submitLabel}</Button>
        </DialogActions>
      </Dialog>
  )
}