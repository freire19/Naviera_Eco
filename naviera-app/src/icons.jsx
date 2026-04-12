/* ═══ NAVIERA — SVG ICON SET ═══ */

const I = ({ d, size = 20, color = "currentColor", strokeWidth = 2 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">
    {typeof d === "string" ? <path d={d} /> : d}
  </svg>
);

export const IconHome = ({ size, color }) => <I size={size} color={color} d={<><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></>} />;
export const IconHeart = ({ size, color }) => <I size={size} color={color} d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />;
export const IconShip = ({ size, color }) => <I size={size} color={color} d={<><path d="M2 20l.8-.8a4 4 0 015.6 0l.4.4a4 4 0 005.6 0l.4-.4a4 4 0 015.6 0l.8.8"/><path d="M4 18l-1-5h18l-1 5"/><path d="M7 13V8h10v5"/><path d="M10 8V5h4v3"/></>} />;
export const IconTicket = ({ size, color }) => <I size={size} color={color} d={<><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></>} />;
export const IconGrid = ({ size, color }) => <I size={size} color={color} d={<><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></>} />;
export const IconCart = ({ size, color }) => <I size={size} color={color} d={<><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/></>} />;
export const IconUsers = ({ size, color }) => <I size={size} color={color} d={<><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></>} />;
export const IconWallet = ({ size, color }) => <I size={size} color={color} d={<><rect x="1" y="4" width="22" height="16" rx="2"/><path d="M1 10h22"/></>} />;
export const IconStore = ({ size, color }) => <I size={size} color={color} d={<><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></>} />;
export const IconUser = ({ size, color }) => <I size={size} color={color} d={<><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></>} />;
export const IconBack = ({ size, color }) => <I size={size} color={color} d={<><polyline points="15,18 9,12 15,6"/></>} />;
export const IconSun = ({ size, color }) => <I size={size} color={color} d={<><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></>} />;
export const IconMoon = ({ size, color }) => <I size={size} color={color} d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />;
export const IconLogout = ({ size, color }) => <I size={size} color={color} d={<><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/></>} />;
export const IconSearch = ({ size, color }) => <I size={size} color={color} d={<><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></>} />;
export const IconCheck = ({ size, color }) => <I size={size} color={color} d={<><polyline points="20,6 9,17 4,12"/></>} />;
export const IconPlus = ({ size, color }) => <I size={size} color={color} d={<><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></>} />;
export const IconRefresh = ({ size, color }) => <I size={size} color={color} d={<><polyline points="23,4 23,10 17,10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></>} />;
export const IconAlert = ({ size, color }) => <I size={size} color={color} d={<><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></>} />;
export const IconMapPin = ({ size, color }) => <I size={size} color={color} d={<><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></>} />;
export const IconPackage = ({ size, color }) => <I size={size} color={color} d={<><line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27,6.96 12,12.01 20.73,6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></>} />;
export const IconClock = ({ size, color }) => <I size={size} color={color} d={<><circle cx="12" cy="12" r="10"/><polyline points="12,6 12,12 16,14"/></>} />;
export const IconCalendar = ({ size, color }) => <I size={size} color={color} d={<><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></>} />;
export const IconEye = ({ size, color }) => <I size={size} color={color} d={<><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></>} />;
export const IconEyeOff = ({ size, color }) => <I size={size} color={color} d={<><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/><path d="M14.12 14.12a3 3 0 11-4.24-4.24"/></>} />;
