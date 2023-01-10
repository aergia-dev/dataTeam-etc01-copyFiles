(ns app.common.utils)

(defn parsing-filename [fname]
  (->> (clojure.string/replace fname #"\s+" "")
                                  ;;  (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((.+)\)"))
       (re-find #"GT_(.+)_?\((.+)\)")))

(defn sort-result [result]
  (sort-by #(-> % second :start) < result))

(defn split-basefile-others [data]
  (prn "data " data)
  {:others (filter (fn [[_ {:keys [:base-file?]}]]
                     (not (true? base-file?))) data)
   :basefile (filter (fn [[_ {:keys [:base-file?]}]]
                       (true? base-file?)) data)})