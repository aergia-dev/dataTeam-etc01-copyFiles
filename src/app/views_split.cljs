(ns app.views_split
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            ["@tauri-apps/api/dialog" :as dialog]
            ["@tauri-apps/api/fs" :as fs]
            [taoensso.timbre :refer [debug info error fatal]]))

