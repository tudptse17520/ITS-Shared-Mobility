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

const VIETMAP_KEY =
  process.env.NEXT_PUBLIC_VIETMAP_API_KEY ||
  "660b3d92e2173b974557cd78118076c8b861dddceb0e8247";

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
  const mapRef = useRef<any>(null);
  const layerGroupRef = useRef<any>(null);
  const leafletRef = useRef<any>(null);
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
        center: [10.7945, 106.7125], // Trung tâm Bình Thạnh - Quận 1 TP.HCM
        zoom: 14,
        zoomControl: true,
        zoomAnimation: !prefersLessMotion,
        fadeAnimation: !prefersLessMotion,
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
      }
    };
  }, []);

useEffect(() => {
    const L = leafletRef.current;
    const map = mapRef.current;
    const layer = layerGroupRef.current;
    if (!mapReady || !L || !map || !layer) return;

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

    function draw() {
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
    }

    draw();
    map.on("zoomend", draw);
    return () => { map.off("zoomend", draw); };
  }, [mapReady, stations, vehicles, selectedVehicleId, telemetryPath, onSelectVehicle]);

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
