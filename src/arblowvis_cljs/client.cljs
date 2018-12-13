(ns arblowvis-cljs.client
  (:require [arblowvis-cljs.thx :as thx]
            [arblowvis-cljs.audio :as aud]))


(defn ^:dev/after-load start []
  (js/console.log "start"))

(defn ^:export init []
  (js/console.log "init")
  (aud/init)
  (thx/init)
  ;(start)
  )

(defn ^:dev/before-load stop []
  (js/console.log "stop"))
