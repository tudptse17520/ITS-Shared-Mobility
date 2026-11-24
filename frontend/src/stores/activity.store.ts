import { create } from "zustand";
import { persist } from "zustand/middleware";

export type ActivityType = "create_user" | "block_user" | "update_config" | "security" | "update_user" | "create_station" | "update_station";

export interface SystemActivity {
  id: string;
  type: ActivityType;
  title: string;
  timestamp: number;
  icon: string;
  color: string;
  bg: string;
}

interface ActivityState {
  activities: SystemActivity[];
  addActivity: (activity: Omit<SystemActivity, "id" | "timestamp">) => void;
}

export const useActivityStore = create<ActivityState>()(
  persist(
    (set) => ({
      activities: [],
      addActivity: (activity) =>
        set((state) => {
          const newActivity: SystemActivity = {
            ...activity,
            id: Math.random().toString(36).substring(7),
            timestamp: Date.now(),
          };
          return { activities: [newActivity, ...state.activities].slice(0, 20) };
        }),
    }),
    {
      name: "its-activity-log-v2",
      version: 1,
      migrate: (persisted) => {
        const state = persisted as Partial<ActivityState>;
        return { activities: state.activities?.filter((activity) => !/^[1-4]$/.test(activity.id)) ?? [] };
      },
    }
  )
);
