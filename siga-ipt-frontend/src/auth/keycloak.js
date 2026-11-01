import Keycloak from 'keycloak-js'

// 'keycloak' = real login. Anything else = 'dev' (simulated login, backend with the dev profile).
export const AUTH_MODE = import.meta.env.VITE_AUTH_MODE === 'keycloak' ? 'keycloak' : 'dev'

let instance = null
let initPromise = null

// The Keycloak object is created lazily so the dev mode never touches it.
const getKeycloak = () => {
  if (!instance) {
    instance = new Keycloak({
      url: import.meta.env.VITE_KEYCLOAK_URL,
      realm: import.meta.env.VITE_KEYCLOAK_REALM,
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
    })
  }
  return instance
}

// keycloak-js can only be initialized once (React StrictMode runs effects twice in dev),
// so the promise is cached. If the user is not logged in, this redirects to the Keycloak login.
export const initKeycloak = () => {
  if (!initPromise) {
    initPromise = getKeycloak().init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    })
  }
  return initPromise
}

// Returns a valid access token, refreshing it when it is about to expire.
export const getAccessToken = async () => {
  const keycloak = getKeycloak()
  try {
    await keycloak.updateToken(30)
  } catch {
    keycloak.login()
  }
  return keycloak.token
}

export const keycloakLogout = () => getKeycloak().logout({ redirectUri: window.location.origin })
