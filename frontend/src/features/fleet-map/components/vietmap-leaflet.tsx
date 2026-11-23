"use client";

import { useEffect, useRef, useState } from "react";
import type { FleetVehicleItem, StationItem, TelemetryLogItem } from "@/services/mobility.service";

interface VietMapLeafletProps {
  stations: StationItem[];
  vehicles: FleetVehicleItem[];
  selectedVehicleId: number | null;
  telemetryPath?: TelemetryLogItem[];
  onSelectVehicle: (vehicle: FleetVehicleItem) => void;
}

const VIETMAP_KEY =
  process.env.NEXT_PUBLIC_VIETMAP_API_KEY ||
  "660b3d92e2173b974557cd78118076c8b861dddceb0e8247";

function getVehicleMarkerColor(status: string): string {
  switch (status) {
    case "AVAILABLE":
      return "#10b981"; // emerald
    case "IN_USE":
      return "#2563eb"; // blue
    case "RESERVED":
      return "#f59e0b"; // amber
    case "LOW_BATTERY":
      return "#f97316"; // orange
    case "MAINTENANCE":
      return "#ef4444"; // red
    default:
      return "#64748b";
  }
}

function loadLeafletAssets(): Promise<any> {
  return new Promise((resolve, reject) => {
    const win = window as any;
    if (win.L) {
      resolve(win.L);
      return;
    }

    if (!document.getElementById("leaflet-css-cdn")) {
      const link = document.createElement("link");
      link.id = "leaflet-css-cdn";
      link.rel = "stylesheet";
      link.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
      document.head.appendChild(link);
    }

    const existingScript = document.getElementById("leaflet-js-cdn") as HTMLScriptElement | null;
    if (existingScript) {
      existingScript.addEventListener("load", () => resolve((window as any).L));
      existingScript.addEventListener("error", reject);
      return;
    }

    const script = document.createElement("script");
    script.id = "leaflet-js-cdn";
    script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
    script.async = true;
    script.onload = () => resolve((window as any).L);
    script.onerror = (err) => reject(err);
    document.body.appendChild(script);
  });
}

export function VietMapLeaflet({
  stations,
  vehicles,
  selectedVehicleId,
  telemetryPath = [],
  onSelectVehicle,
}: VietMapLeafletProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const layerGroupRef = useRef<any>(null);
  const leafletRef = useRef<any>(null);
  const [mapReady, setMapReady] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function initMap() {
      if (!containerRef.current || mapRef.current) return;

      const L = await loadLeafletAssets().catch(() => null);
      if (!L || !isMounted || !containerRef.current) return;

      leafletRef.current = L;

      const map = L.map(containerRef.current, {
        center: [10.7945, 106.7125], // Trung tâm Bình Thạnh - Quận 1 TP.HCM
        zoom: 14,
        zoomControl: true,
      });

      // Nạp lớp bản đồ VietMap Raster Tile chính thức với API Key
      L.tileLayer(
        `https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${VIETMAP_KEY}`,
        {
          attribution: "&copy; VietMap ITS Shared Mobility",
          maxZoom: 20,
          tileSize: 256,
        }
      ).addTo(map);

      const group = L.layerGroup().addTo(map);
      mapRef.current = map;
      layerGroupRef.current = group;
      setMapReady(true);
    }

    initMap();

    return () => {
      isMounted = false;
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    const L = leafletRef.current;
    const map = mapRef.current;
    const group = layerGroupRef.current;
    if (!L || !map || !group) return;

    group.clearLayers();

    // 1. Vẽ các Trạm xe công cộng (Stations)
    stations.forEach((st) => {
      const stationIcon = L.divIcon({
        className: "its-station-pin",
        html: `<div style="background:#0f172a;color:#fff;border:2px solid #38bdf8;border-radius:10px;padding:3px 8px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 3px 10px rgba(0,0,0,0.35);">Hub · ${st.availableVehicles}/${st.totalDocks}</div>`,
        iconSize: [110, 24],
        iconAnchor: [55, 12],
      });

      L.marker([st.latitude, st.longitude], { icon: stationIcon })
        .bindPopup(
          `<div style="font-family:sans-serif;min-width:190px">
            <strong>${st.stationName}</strong><br/>
            <span style="color:#64748b;font-size:12px">${st.address}</span><br/>
            <span style="color:#0284c7;font-weight:600;font-size:12px">Xe sẵn sàng: ${st.availableVehicles} / ${st.totalDocks} chỗ</span>
          </div>`
        )
        .addTo(group);
    });

    // 2. Vẽ lộ trình Telemetry (Polyline) nếu có của xe đang chọn
    if (telemetryPath.length > 1) {
      const latlngs = [...telemetryPath]
        .reverse()
        .map((pt) => [pt.latitude, pt.longitude]);
      L.polyline(latlngs, {
        color: "#2563eb",
        weight: 4,
        opacity: 0.85,
        dashArray: "6 6",
      }).addTo(group);
    }

    // 3. Vẽ các Phương tiện trong Đội xe (Fleet Vehicles)
    vehicles.forEach((v) => {
      const color = getVehicleMarkerColor(v.fleetStatus);
      const isSelected = v.id === selectedVehicleId;
      const border = isSelected ? "3px solid #facc15" : "2px solid #ffffff";
      const scale = isSelected ? "transform:scale(1.15);" : "";

      const vehicleIcon = L.divIcon({
        className: "its-vehicle-pin",
        html: `<div style="display:flex;align-items:center;gap:4px;background:${color};color:#fff;border:${border};border-radius:999px;padding:3px 8px;font-size:11px;font-weight:700;white-space:nowrap;box-shadow:0 3px 10px rgba(0,0,0,0.3);${scale}">
          <span>${v.vehicleCode}</span>
          <span style="opacity:.9">${v.batteryLevel}%</span>
        </div>`,
        iconSize: [108, 26],
        iconAnchor: [54, 13],
      });

      const marker = L.marker([v.latitude, v.longitude], { icon: vehicleIcon })
        .on("click", () => onSelectVehicle(v))
        .bindPopup(
          `<div style="font-family:sans-serif;min-width:200px">
            <strong>${v.vehicleCode} · ${v.brand}</strong><br/>
            <span style="font-size:12px;color:#475569">Loại: ${v.vehicleTypeName || "-"}</span><br/>
            <span style="font-size:12px">Pin: <b>${v.batteryLevel}%</b> · Khóa: <b>${v.lockStatus}</b></span><br/>
            <span style="font-size:12px">Trạng thái: <b>${v.fleetStatus}</b></span>
          </div>`
        );

      marker.addTo(group);
    });
  }, [mapReady, stations, vehicles, selectedVehicleId, telemetryPath, onSelectVehicle]);

  return (
    <div
      ref={containerRef}
      className="h-[540px] w-full rounded-xl border border-border/50 overflow-hidden z-0"
    />
  );
}
