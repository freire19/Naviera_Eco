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

      onConnect: () => {
        setConnected(true);
        reconnectDelay.current = 2000; // reset backoff

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
        // exponential backoff capped at 30s
        reconnectDelay.current = Math.min(reconnectDelay.current * 2, 30000);
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
