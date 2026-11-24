"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import type { FleetVehicleItem, StationItem, TelemetryLogItem } from "@/services/mobility.service";

interface VietMapLeafletProps {
  className?: string;
  stations: StationItem[];
  vehicles: FleetVehicleItem[];
  selectedVehicleId: number | null;
  telemetryPath?: TelemetryLogItem[];
  onSelectVehicle: (vehicle: FleetVehicleItem) => void;
}

interface LeafletLayer {
  addTo: (target: LeafletMap | LeafletLayerGroup) => LeafletLayer;
  bindPopup: (html: string | HTMLElement, options?: Record<string, unknown>) => LeafletLayer;
  on: (event: string, handler: () => void) => LeafletLayer;
}

interface LeafletLayerGroup {
  addTo: (map: LeafletMap) => LeafletLayerGroup;
  clearLayers: () => void;
}

interface LeafletPoint {
  distanceTo: (other: LeafletPoint) => number;
}

interface LeafletMap {
  remove: () => void;
  setView: (center: [number, number], zoom: number, options?: { animate?: boolean }) => void;
  invalidateSize: (options?: { pan?: boolean }) => void;
  project: (latLng: [number, number]) => LeafletPoint;
  on: (event: string, handler: () => void) => void;
  off: (event: string, handler: () => void) => void;
}

interface LeafletStatic {
  map: (el: HTMLElement, options: Record<string, unknown>) => LeafletMap;
  tileLayer: (url: string, options: Record<string, unknown>) => LeafletLayer;
  layerGroup: () => LeafletLayerGroup;
  circleMarker: (latLng: [number, number], options: Record<string, unknown>) => LeafletLayer;
  marker: (latLng: [number, number], options: Record<string, unknown>) => LeafletLayer;
  divIcon: (options: Record<string, unknown>) => unknown;
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
      return "#217756"; // emerald
    case "IN_USE":
      return "#395e8d"; // blue
    case "RESERVED":
      return "#8b5c27"; // amber
    case "LOW_BATTERY":
      return "#9c4d2b"; // orange
    case "MAINTENANCE":
      return "#a63640"; // red
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

const emptyTelemetry: TelemetryLogItem[] = [];

export function VietMapLeaflet({
  className,
  stations,
  vehicles,
  selectedVehicleId,
  telemetryPath = emptyTelemetry,
  onSelectVehicle,
}: VietMapLeafletProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const layerGroupRef = useRef<LeafletLayerGroup | null>(null);
  const leafletRef = useRef<LeafletStatic | null>(null);
  const [mapReady, setMapReady] = useState(false);
  const [mapError, setMapError] = useState(false);

  useEffect(() => {
    let isMounted = true;
    let observer: ResizeObserver | undefined;

    async function initMap() {
      if (!containerRef.current || mapRef.current) return;

      const L = await loadLeafletAssets().catch(() => null);
      if (!isMounted || !containerRef.current) return;
      if (!L) { setMapError(true); return; }

      leafletRef.current = L;

      const prefersLessMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      const map = L.map(containerRef.current, {
        center: [10.7945, 106.7125],
        zoom: 14,
        zoomControl: true,
        zoomAnimation: !prefersLessMotion,
        fadeAnimation: !prefersLessMotion,
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
      observer = new ResizeObserver(() => map.invalidateSize({ pan: false }));
      observer.observe(containerRef.current);
      layerGroupRef.current = group;
      setMapReady(true);
    }

    initMap();

    return () => {
      isMounted = false;
      observer?.disconnect();
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
        layerGroupRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    const maybeL = leafletRef.current;
    const maybeMap = mapRef.current;
    const maybeLayer = layerGroupRef.current;
    if (!mapReady || !maybeL || !maybeMap || !maybeLayer) return;
    const L = maybeL;
    const map = maybeMap;
    const layer = maybeLayer;

    type MapItem = {
      location: [number, number];
      title: string;
      detail: string;
      vehicle?: FleetVehicleItem;
    };

    const points: MapItem[] = [
      ...stations.map((station): MapItem => ({
        location: [station.latitude, station.longitude],
        title: station.stationName,
        detail: station.availableVehicles + " xe sẵn sàng · " + station.address,
      })),
      ...vehicles.map((vehicle): MapItem => ({
        location: [vehicle.latitude, vehicle.longitude],
        title: vehicle.vehicleCode + " · " + vehicle.brand,
        detail: "Pin " + vehicle.batteryLevel + "%",
        vehicle,
      })),
    ];

    const draw = () => {
      layer.clearLayers();
      if (telemetryPath.length > 1) {
        L.polyline([...telemetryPath].reverse().map((point) => [point.latitude, point.longitude]), {
          color: "#236b60", weight: 4, opacity: 0.85, dashArray: "6 6",
        }).addTo(layer);
      }

      const groups: MapItem[][] = [];
      for (const item of points) {
        const pixel = map.project(item.location);
        const nearby = groups.find((group) => map.project(group[0].location).distanceTo(pixel) < 49);
        if (nearby) nearby.push(item);
        else groups.push([item]);
      }

      for (const group of groups) {
        const targets = group.flatMap((item) => item.vehicle ? [item.vehicle] : []);
        const selected = targets.some((vehicle) => vehicle.id === selectedVehicleId);
        const label = targets.length ? targets.length + " xe" : "T";
        const markerTitle = targets.length ? targets.length + " xe trên bản đồ" : group[0].title;
        const palette = targets.length === 1 ? getVehicleMarkerColor(targets[0].fleetStatus) : "#343841";
        const markerText = document.createElement("span");
        markerText.className = "mobility-map-dot";
        markerText.dataset.selected = String(selected);
        markerText.style.backgroundColor = palette;
        markerText.textContent = label;

        const popup = document.createElement("div");
        popup.className = "mobility-map-popup";
        for (const item of group) {
          const row = document.createElement("div");
          if (item.vehicle) {
            const button = document.createElement("button");
            const vehicle = item.vehicle;
            button.type = "button";
            button.textContent = item.title;
            button.addEventListener("click", () => onSelectVehicle(vehicle));
            row.append(button);
          } else {
            const title = document.createElement("strong");
            title.textContent = item.title;
            row.append(title);
          }
          const detail = document.createElement("small");
          detail.textContent = item.detail;
          row.append(detail);
          popup.append(row);
        }

        L.marker(group[0].location, {
          icon: L.divIcon({ className: "mobility-map-marker", html: markerText, iconSize: [36, 30], iconAnchor: [18, 15] }),
          title: markerTitle, alt: markerTitle, zIndexOffset: selected ? 1000 : 0,
        }).bindPopup(popup, { maxWidth: 250 }).addTo(layer);
      }
    };

    draw();
    map.on("zoomend", draw);
    return () => { map.off("zoomend", draw); };
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
    <div
      ref={containerRef}
      className={cn("relative z-0 h-[clamp(340px,56dvh,590px)] w-full overflow-hidden rounded-xl border bg-muted", className)}
      aria-label="Bản đồ xe và trạm"
    >
      {mapError && <p className="p-4 text-sm text-muted-foreground">Không tải được bản đồ. Bạn vẫn có thể chọn xe trong danh sách.</p>}
    </div>
  );
}
