import { createContext, useCallback, useContext, useState } from 'react'
import { Alert, Snackbar } from '@mui/material'

const NotifyContext = createContext(() => {})

export function NotifyProvider({ children }) {
  const [state, setState] = useState({ open: false, message: '', severity: 'success' })

  const notify = useCallback((message, severity = 'success') => {
    setState({ open: true, message, severity })
  }, [])

  return (
    <NotifyContext.Provider value={notify}>
      {children}
      <Snackbar
        open={state.open}
        autoHideDuration={6000}
        onClose={() => setState((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={state.severity} variant="filled" onClose={() => setState((s) => ({ ...s, open: false }))}>
          {state.message}
        </Alert>
      </Snackbar>
    </NotifyContext.Provider>
  )
}

export const useNotify = () => useContext(NotifyContext)
