import { useState, useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";

/**
 * useWebSocket — STOMP over SockJS for real-time notifications.
 *
 * @param {{ token: string|null, empresaId: string|number|null, apiUrl: string }} opts
 * @returns {{ connected: boolean, notifications: Array, clearNotifications: Function, unreadCount: number }}
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

    // Derive WS url: http://…/api -> http://…/api/ws
    const wsUrl = `${apiUrl}/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: reconnectDelay.current,
      // #319: STOMP heartbeats (ms) — sem isso, NAT/celular mantem conexao TCP "viva" mas
      //   sem dados o broker nao detecta que o cliente sumiu (zombie). 10s e o sweet spot
      //   para mobile (bateria) sem perder o evento de queda.
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        setConnected(true);
        // #026: reseta tanto o ref (proxima onStompError) quanto o client.reconnectDelay
        //   que e o valor realmente lido pelo STOMP em reconexoes.
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
        // #026: backoff exponencial ate 30s — sem atualizar client.reconnectDelay o STOMP
        //   mantem o valor inicial (2s) e reconecta em loop tight em falhas repetidas.
        reconnectDelay.current = Math.min(reconnectDelay.current * 2, 30000);
        client.reconnectDelay = reconnectDelay.current;
      },

      onWebSocketError: () => {
        // graceful degradation — app keeps working
        setConnected(false);
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token, empresaId, apiUrl]);

  return { connected, notifications, clearNotifications, unreadCount };
}
