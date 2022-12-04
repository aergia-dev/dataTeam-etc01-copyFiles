(ns app.tauri-cmd
  (:require ["@tauri-apps/api/tauri" :refer [invoke]]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;; (defn call-fn [name param]
;;   (let [invoke js/window.__TAURI__.invoke]
;;     ;; (-> invoke name (clj->js param)
;;         (-> (invoke "custom_command" (clj->js {"value" "dkdkdk"}))
;;         (.then #(prn "return of " name ": " %))
;;         (.catch #(prn "catch - exec of " name ": " %)))))

;; (defn call-fn2 [name param]
;;   (go
;;     (let [invoke js/window.__TAURI__.invoke]
;;       (try (<p! (invoke name (clj->js param)))
;;            (catch js/Error err (js/console.log (ex-cause err)))))))



;; (cmd/call-fn "custom-command" {:value "dkdkdkdkdk"})

;;(defn )
