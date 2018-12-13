(ns arblowvis-cljs.client
  (:require [arblowvis-cljs.thx :as thx]
            ))


(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn ^:export init []
  (js/console.log "init")
  (thx/init)
  ;(start)
  )

(defn ^:dev/before-load stop []
  (js/console.log "stop"))
