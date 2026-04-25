import { useState, useEffect, useRef, useCallback } from "react";

/**
 * useWebSocket — STOMP over SockJS for real-time notifications.
 *
 * STOMP/SockJS sao import()-dinamicos: ~50KB ficam fora do bundle inicial e so
 * carregam quando ha empresaId ativo (nunca para clients CPF/CNPJ no naviera-app).
 *
 * @param {{ token: string|null, empresaId: string|number|null, apiUrl: string }} opts
 */
export default function useWebSocket({ token, empresaId, apiUrl }) {
  const [connected, setConnected] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const clientRef = useRef(null);
  const reconnectDelay = useRef(2000);
  const MAX_NOTIFICATIONS = 50;

  const clearNotifications = useCallback(() => setNotifications([]), []);

  const unreadCount = notifications.length;

  useEffect(() => {
    if (!token || !empresaId) return;

    let cancelled = false;
    let client = null;

    Promise.all([
      import("@stomp/stompjs"),
      import("sockjs-client/dist/sockjs"),
    ]).then(([{ Client }, SockJSMod]) => {
      if (cancelled) return;
      const SockJS = SockJSMod.default || SockJSMod;
      const wsUrl = `${apiUrl}/ws`;

      client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: reconnectDelay.current,
        // #319: heartbeats 10s — NAT/celular mantem TCP "vivo" sem dados; sem isso o
        //   broker nao detecta cliente zombie. 10s equilibra bateria mobile e deteccao.
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        onConnect: () => {
          setConnected(true);
          // #026: reseta ref e client.reconnectDelay (valor lido em reconexoes).
          reconnectDelay.current = 2000;
          client.reconnectDelay = 2000;

          client.subscribe(
            `/topic/empresa/${empresaId}/notifications`,
            (message) => {
              try {
                const payload = JSON.parse(message.body);
                setNotifications((prev) => [payload, ...prev].slice(0, MAX_NOTIFICATIONS));
              } catch {
                // ignore malformed messages
              }
            }
          );
        },

        onDisconnect: () => setConnected(false),

        onStompError: (frame) => {
          console.warn("[WS] STOMP error", frame.headers?.message);
          setConnected(false);
          // #026: backoff exponencial ate 30s — sem atualizar reconnectDelay o STOMP
          //   mantem 2s inicial e cai em loop tight em falhas repetidas.
          reconnectDelay.current = Math.min(reconnectDelay.current * 2, 30000);
          client.reconnectDelay = reconnectDelay.current;
        },

        onWebSocketError: () => setConnected(false),
      });

      clientRef.current = client;
      client.activate();
    }).catch(e => console.warn("[WS] falha ao carregar STOMP/SockJS:", e?.message));

    return () => {
      cancelled = true;
      if (client) client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token, empresaId, apiUrl]);

  return { connected, notifications, clearNotifications, unreadCount };
}
