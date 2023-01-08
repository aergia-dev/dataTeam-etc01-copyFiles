(ns app.common.utils)

(defn parsing-filename [fname]
  (->> (clojure.string/replace fname #"\s+" "")
                                  ;;  (re-find #"\d+_\d+_\d+_\d+_\d+_\d+_(.+)_?\((.+)\)"))
       (re-find #"GT_(.+)_?\((.+)\)")))

