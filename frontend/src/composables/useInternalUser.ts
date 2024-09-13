import useSessionStore from "@/stores/sessionStore"

export function useInternalUser() {
  const session = useSessionStore()
  return session.user?.roles?.includes("Internal") ?? false
}
