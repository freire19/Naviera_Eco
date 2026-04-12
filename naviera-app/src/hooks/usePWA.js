import { useState, useEffect, useCallback } from "react";

export default function usePWA() {
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [isInstalled, setIsInstalled] = useState(false);
  const [dismissed, setDismissed] = useState(() => {
    try { return sessionStorage.getItem("naviera_pwa_dismissed") === "1"; }
    catch { return false; }
  });

  useEffect(() => {
    // Check if already installed (standalone mode)
    const mq = window.matchMedia("(display-mode: standalone)");
    setIsInstalled(mq.matches || navigator.standalone === true);

    const onChange = (e) => setIsInstalled(e.matches);
    mq.addEventListener("change", onChange);

    // Capture install prompt
    const onBeforeInstall = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };
    window.addEventListener("beforeinstallprompt", onBeforeInstall);

    // Detect successful install
    const onInstalled = () => {
      setIsInstalled(true);
      setDeferredPrompt(null);
    };
    window.addEventListener("appinstalled", onInstalled);

    return () => {
      mq.removeEventListener("change", onChange);
      window.removeEventListener("beforeinstallprompt", onBeforeInstall);
      window.removeEventListener("appinstalled", onInstalled);
    };
  }, []);

  const canInstall = !!deferredPrompt && !isInstalled && !dismissed;

  const promptInstall = useCallback(async () => {
    if (!deferredPrompt) return false;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    setDeferredPrompt(null);
    return outcome === "accepted";
  }, [deferredPrompt]);

  const dismiss = useCallback(() => {
    setDismissed(true);
    try { sessionStorage.setItem("naviera_pwa_dismissed", "1"); } catch {}
  }, []);

  return { isInstalled, canInstall, promptInstall, dismiss };
}
