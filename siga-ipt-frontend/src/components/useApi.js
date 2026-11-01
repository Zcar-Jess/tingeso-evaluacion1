import { useCallback, useEffect, useState } from 'react'
import { errorMessage } from '../api/http-common'
import { useNotify } from './Notify'

// Loads data from a fetcher (a function returning an axios promise, or null to skip).
export default function useApi(fetcher, deps = [], initial = []) {
  const notify = useNotify()
  const [data, setData] = useState(initial)
  const [loading, setLoading] = useState(true)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reload = useCallback(async () => {
    const request = fetcher()
    if (!request) {
      setData(initial)
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      const response = await request
      setData(response.data)
    } catch (error) {
      notify(errorMessage(error), 'error')
    } finally {
      setLoading(false)
    }
  }, deps)

  useEffect(() => {
    reload()
  }, [reload])

  return { data, loading, reload }
}
