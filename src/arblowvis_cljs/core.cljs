(ns arblowvis-cljs.core
  (:require [arblowvis-cljs.thx :as thx]
            [arblowvis-cljs.audio :as aud]))
(enable-console-print!)

(println "This text is printed from src/arblowvis-cljs/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(thx/init)
(aud/init)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
