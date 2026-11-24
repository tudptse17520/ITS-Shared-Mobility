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

interface LeafletLayer {
  addTo: (target: LeafletMap | LeafletLayerGroup) => LeafletLayer;
  bindPopup: (html: string) => LeafletLayer;
  on: (event: string, handler: () => void) => LeafletLayer;
}

interface LeafletLayerGroup {
  addTo: (map: LeafletMap) => LeafletLayerGroup;
  clearLayers: () => void;
}

interface LeafletMap {
  remove: () => void;
  setView: (center: [number, number], zoom: number, options?: { animate?: boolean }) => void;
}

interface LeafletStatic {
  map: (el: HTMLElement, options: Record<string, unknown>) => LeafletMap;
  tileLayer: (url: string, options: Record<string, unknown>) => LeafletLayer;
  layerGroup: () => LeafletLayerGroup;
  circleMarker: (latLng: [number, number], options: Record<string, unknown>) => LeafletLayer;
  polyline: (coords: [number, number][], options: Record<string, unknown>) => LeafletLayer;
}

declare global {
  interface Window {
    L?: LeafletStatic;
  }
}

const VIETMAP_KEY = process.env.NEXT_PUBLIC_VIETMAP_API_KEY || "";

function getVehicleMarkerColor(status: string): string {
  switch (status) {
    case "AVAILABLE":
      return "#10b981";
    case "IN_USE":
      return "#2563eb";
    case "RESERVED":
      return "#f59e0b";
    case "LOW_BATTERY":
      return "#f97316";
    case "MAINTENANCE":
      return "#ef4444";
    default:
      return "#64748b";
  }
}

function loadLeafletAssets(): Promise<LeafletStatic> {
  return new Promise((resolve, reject) => {
    if (window.L) {
      resolve(window.L);
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
      existingScript.addEventListener("load", () => {
        if (window.L) resolve(window.L);
        else reject(new Error("Leaflet failed to initialize"));
      });
      existingScript.addEventListener("error", reject);
      return;
    }

    const script = document.createElement("script");
    script.id = "leaflet-js-cdn";
    script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
    script.async = true;
    script.onload = () => {
      if (window.L) resolve(window.L);
      else reject(new Error("Leaflet failed to initialize"));
    };
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
  const mapRef = useRef<LeafletMap | null>(null);
  const layerGroupRef = useRef<LeafletLayerGroup | null>(null);
  const leafletRef = useRef<LeafletStatic | null>(null);
  const [mapReady, setMapReady] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function initMap() {
      if (!containerRef.current || mapRef.current) return;

      const L = await loadLeafletAssets().catch(() => null);
      if (!L || !isMounted || !containerRef.current) return;

      leafletRef.current = L;

      const map = L.map(containerRef.current, {
        center: [10.7945, 106.7125],
        zoom: 14,
        zoomControl: true,
      });

      const tileUrl = VIETMAP_KEY
        ? `https://maps.vietmap.vn/api/tm/{z}/{x}/{y}@2x.png?apikey=${VIETMAP_KEY}`
        : "https://tile.openstreetmap.org/{z}/{x}/{y}.png";

      L.tileLayer(tileUrl, {
        attribution: VIETMAP_KEY ? "&copy; VietMap ITS Shared Mobility" : "&copy; OpenStreetMap contributors",
        maxZoom: 20,
        tileSize: 256,
      }).addTo(map);

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
        layerGroupRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    const L = leafletRef.current;
    const group = layerGroupRef.current;
    const map = mapRef.current;
    if (!mapReady || !L || !group || !map) return;

    group.clearLayers();

    stations.forEach((st) => {
      const marker = L.circleMarker([st.latitude, st.longitude], {
        radius: 10,
        color: "#4338ca",
        fillColor: "#6366f1",
        fillOpacity: 0.35,
        weight: 2,
      }).addTo(group);
      marker.bindPopup(
        `<b>${st.stationName}</b><br/>${st.address}<br/>Xe sẵn sàng: <b>${st.availableVehicles}</b> / ${st.totalDocks} dock`
      );
    });

    if (telemetryPath.length > 1) {
      const coords = [...telemetryPath]
        .reverse()
        .map((pt): [number, number] => [pt.latitude, pt.longitude]);
      L.polyline(coords, {
        color: "#0284c7",
        weight: 4,
        opacity: 0.8,
        dashArray: "6 6",
      }).addTo(group);
    }

    vehicles.forEach((v) => {
      const isSelected = v.id === selectedVehicleId;
      const color = getVehicleMarkerColor(v.fleetStatus);
      const marker = L.circleMarker([v.latitude, v.longitude], {
        radius: isSelected ? 10 : 7,
        color: isSelected ? "#0f172a" : "#ffffff",
        fillColor: color,
        fillOpacity: 0.95,
        weight: isSelected ? 3 : 2,
      }).addTo(group);

      marker.on("click", () => onSelectVehicle(v));
      marker.bindPopup(
        `<b>${v.vehicleCode}</b> (${v.brand})<br/>Trạng thái: <b>${v.fleetStatus}</b> · Pin: <b>${v.batteryLevel}%</b><br/>Khóa: ${v.lockStatus}`
      );
    });
  }, [mapReady, stations, vehicles, selectedVehicleId, telemetryPath, onSelectVehicle]);

  useEffect(() => {
    const map = mapRef.current;
    if (!mapReady || !map || !selectedVehicleId) return;
    const target = vehicles.find((v) => v.id === selectedVehicleId);
    if (target) {
      map.setView([target.latitude, target.longitude], 15, { animate: true });
    }
  }, [mapReady, selectedVehicleId, vehicles]);

  return (
    <div className="relative h-[520px] w-full overflow-hidden rounded-xl border bg-slate-100 dark:bg-slate-900">
      <div ref={containerRef} className="h-full w-full z-0" />
      {!mapReady && (
        <div className="absolute inset-0 flex items-center justify-center bg-background/70 text-sm text-muted-foreground">
          Đang tải bản đồ VietMap TP.HCM...
        </div>
      )}
    </div>
  );
}
