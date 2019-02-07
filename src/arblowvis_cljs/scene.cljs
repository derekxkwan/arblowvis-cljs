(ns arblowvis-cljs.scene
  (:require [arblowvis-cljs.audio :as aud]
            ))

(def th js/THREE)
(def th-x js/THREEx)
(def h-pi (/ js/Math.PI 2.0))
(def w-pi js/Math.PI)
(def online? true)

(def bell-sheet "res/bell_sheet.png")
(def wine-sheet "res/wine_sheet.png")
(def gas-sheet "res/gas_sheet.png")
(def food-sheet "res/food_sheet.png")

(def bell-url "res/bell3.ply")
(def wine-url "res/wine2.ply")
(def gas-url "res/gas_station.ply")
(def food-url "res/fingerfood.ply")

(def debug? true)
(def txr-loader nil)
(def ply-loader nil)


(def bell-mesh nil)
(def wine-mesh nil)
(def gas-mesh nil)
(def food-mesh nil)

(def dir-light nil)
(def amb-light nil)


(def on-render-fns (atom []))
(def update-fns (atom []))
(def cur-opacity (atom 0))
(def ramp-val 0.002)
(def visible-thresh 0)


(def rms-num 20)
(def rms-vals (atom ((comp vec (partial take rms-num) (partial cycle)) '(0))))
(def rms-idx (atom 0))
(def low-rms 0.2)
(def hi-rms 0.35)

(def min-rms (atom 0))
(def max-rms (atom 0)) ;; around 0.6?

(defn parse-path [cur-path]
  (if (true? online?)
    cur-path
    (str "/../" cur-path)
    )
  )
  

(defn rms-idx-inc [cur-idx]
  (mod (inc cur-idx) rms-num)
  )

(defn reset-rms-history []
  (reset! rms-vals ((comp vec (partial take rms-num) (partial cycle)) '(0)))
  (reset! rms-idx 0)
  )

(defn get-rms-avg []
  (let [cur-sum (apply + @rms-vals)]
    (/ cur-sum rms-num)
    )
  )

(defn update-rms-avg [cur-rms]
  (let [cur-rms (aud/get-rms)
        cur-idx @rms-idx]
    (swap! rms-vals assoc cur-idx cur-rms)
    (swap! rms-idx rms-idx-inc)
    )
  )


(defn rms-scaler [input-val]
  (let [cur-range (- hi-rms low-rms)
        cur-scaler (/ 1.0 cur-range)
        cur-scaled (* (- input-val low-rms) cur-scaler)]
    (min 1.0 (max 0.0 cur-scaled))
    )
  )

(defn update-rms []
      ;;scale opacity here from rms given range of rms is 0 to 0.5
  (update-rms-avg (aud/get-rms))
  (let [cur-avg (get-rms-avg)
        scaled-avg (rms-scaler cur-avg)
        prev-opacity @cur-opacity]
    ;(when (> cur-rms @max-rms) (reset! max-rms cur-rms))
    ;(when (< cur-rms @min-rms) (reset! min-rms cur-rms))
    ;(.log js/console @min-rms @max-rms)
    ;(.log js/console scaled-avg)
    (if (> scaled-avg prev-opacity)
      (reset! cur-opacity scaled-avg)
      (swap! cur-opacity (fn [x] (max 0 (- x ramp-val))))
      )
    )
  ;(.log js/console @cur-opacity)
  )

(defn animate-texture [cur-txr num-horiz num-vert num-total cur-dur]
  (js/TextureAnimator. cur-txr num-horiz num-vert num-total cur-dur)
  )

(defn load-txr [^:js/String txr-url]
  (let [cur-txr (.load txr-loader txr-url)
        ]
    (set! (.-wrapS cur-txr) th.RepeatWrapping)
    (set! (.-wrapT cur-txr) th.RepeatWrapping)
    cur-txr
  )
  )

(defn init-txr-loader []
  (.log js/console "init txr loader")
  (set! txr-loader (th.TextureLoader.))
  )

(defn load-ply [^:js/String ply-url cur-callback]
  (.load ply-loader ply-url cur-callback)
  )

(defn init-ply-loader []
  (.log js/console "init ply loader")
  (set! ply-loader (th.PLYLoader.))
  )

(defn init-lights [cur-scene]
  (set! dir-light (th.DirectionalLight. 0xffffff 3.0))
  (set! amb-light (th.AmbientLight. 0xffffff))
  ;(.set (.-position dir-light) 0 3 3)
  (.add cur-scene amb-light)
  (.add cur-scene dir-light)
  )


(defn init-bell [cur-root cur-ply cur-pic]
  (.log js/console "init bell")
  (load-ply cur-ply
         (fn [cur-geom]
           (.computeVertexNormals cur-geom)
           (let [cur-txr (load-txr cur-pic)
                 cur-anim (animate-texture cur-txr 20 1 20 300)
                 cur-mat (th.MeshBasicMaterial.
                          (js-obj "map" cur-txr
                                  "side" th.DoubleSide
                                  "flatShading" false))
                 cur-mesh (th.Mesh. cur-geom cur-mat)]
             (.add cur-root cur-mesh)
             (.set (.-scale cur-mesh) 0.25 0.25 0.25)
             (set! (-> cur-mesh .-rotation .-x) (* w-pi -1))
             (set! (-> cur-mesh .-position .-y) 0.5)
             (set! (-> cur-mesh .-position .-z) 1)

             (set! (.-castShadow cur-mesh) true)
             (swap! update-fns conj
                    (fn [cur-delta]
                      (.update cur-anim (* 1000 cur-delta))
                      (if (true? debug?)
                        (do (set! (-> cur-mesh .-visible) true)
                            (set! (-> cur-mesh .-material .-opacity) 1))
                        (when (true? (.-visible cur-root))
                          (let [cur-op @cur-opacity]
                            (if (< cur-op visible-thresh) (set! (-> cur-mesh .-visible) false)
                                (do
                                  (set! (-> cur-mesh .-visible) true)
                                  (set! (-> cur-mesh .-material .-opacity) @cur-opacity))
                                )
                            
                            )
                          )
                        )

                      )

                    
                    )

             cur-mesh
             )
           )
         )
  )


(defn init-wine [cur-root cur-ply cur-pic]
  (.log js/console "init wine")
  (load-ply cur-ply
         (fn [cur-geom]
           (.computeVertexNormals cur-geom)
           (let [cur-txr (load-txr cur-pic)
                 cur-anim (animate-texture cur-txr 20 1 20 300)
                 cur-mat (th.MeshStandardMaterial.
                          (js-obj "map" cur-txr
                                  "side" th.DoubleSide
                                  "flatShading" false))
                 cur-mesh (th.Mesh. cur-geom cur-mat)]
             (.add cur-root cur-mesh)
             (.set (.-scale cur-mesh) 0.35 0.35 0.35)
             (set! (-> cur-mesh .-rotation .-x) (* w-pi -1))
             (set! (-> cur-mesh .-position .-y) 1)
             (set! (-> cur-mesh .-position .-z) 1)
             (set! (.-castShadow cur-mesh) true)
             (swap! update-fns conj
                    (fn [cur-delta]
                      (.update cur-anim (* 1000 cur-delta))
                      (if (true? debug?)
                        (do (set! (-> cur-mesh .-visible) true)
                            (set! (-> cur-mesh .-material .-opacity) 1))
                        (when (true? (.-visible cur-root))
                          (let [cur-op @cur-opacity]
                            (if (< cur-op visible-thresh) (set! (-> cur-mesh .-visible) false)
                                (do
                                  (set! (-> cur-mesh .-visible) true)
                                  (set! (-> cur-mesh .-material .-opacity) @cur-opacity))
                                )
                            
                            )
                          )
                        )

                      )
                    )

             cur-mesh
             )
           )
         )
  )


(defn init-gas [cur-root cur-ply cur-pic]
  (.log js/console "init gas")
  (load-ply cur-ply
         (fn [cur-geom]
           (.computeVertexNormals cur-geom)
           (let [cur-txr (load-txr cur-pic)
                 cur-anim (animate-texture cur-txr 20 1 20 300)
                 cur-mat (th.MeshStandardMaterial.
                          (js-obj "map" cur-txr
                                  "side" th.DoubleSide
                                  "flatShading" false
                                  ))
                 cur-mesh (th.Mesh. cur-geom cur-mat)]
             (.add cur-root cur-mesh)
             (.set (.-scale cur-mesh) 0.35 0.35 0.35)
             (set! (-> cur-mesh .-rotation .-x) (* w-pi -1))
             (set! (-> cur-mesh .-rotation .-z) (* w-pi 1.5))

             ;(set! (-> cur-mesh .-position .-y) 1)
             ;(set! (-> cur-mesh .-position .-z) -1)
             ;(set! (.-castShadow cur-mesh) true)
             (swap! update-fns conj
                    (fn [cur-delta]
                      (.update cur-anim (* 1000 cur-delta))
                      (if (true? debug?)
                        (do (set! (-> cur-mesh .-visible) true)
                            (set! (-> cur-mesh .-material .-opacity) 1))
                        (when (true? (.-visible cur-root))
                          (let [cur-op @cur-opacity]
                            (if (< cur-op visible-thresh) (set! (-> cur-mesh .-visible) false)
                                (do
                                  (set! (-> cur-mesh .-visible) true)
                                  (set! (-> cur-mesh .-material .-opacity) @cur-opacity))
                                )
                            
                            )
                          )
                        )

                      )
                    )

             cur-mesh
             )
           )
         )
  )
                      
  (defn init-food [cur-root cur-ply cur-pic]
  (.log js/console "init food")
  (load-ply cur-ply
         (fn [cur-geom]
           (.computeVertexNormals cur-geom)
           (let [cur-txr (load-txr cur-pic)
                 cur-anim (animate-texture cur-txr 13 1 13 300)
                 cur-mat (th.MeshStandardMaterial.
                          (js-obj "map" cur-txr
                                  "side" th.DoubleSide
                                  "flatShading" false
                                  ))
                 cur-mesh (th.Mesh. cur-geom cur-mat)]
             (.add cur-root cur-mesh)
             (.set (.-scale cur-mesh) 0.3 0.3 0.3)
             (set! (-> cur-mesh .-rotation .-x) (* w-pi -1))
             (set! (-> cur-mesh .-rotation .-z) (* w-pi 0.75))

             ;(set! (-> cur-mesh .-position .-y) 1)
             ;(set! (-> cur-mesh .-position .-z) -1)
             ;(set! (.-castShadow cur-mesh) true)
             (swap! update-fns conj
                    (fn [cur-delta]
                      (.update cur-anim (* 1000 cur-delta))
                      (if (true? debug?)
                        (do (set! (-> cur-mesh .-visible) true)
                            (set! (-> cur-mesh .-material .-opacity) 1))
                        (when (true? (.-visible cur-root))
                          (let [cur-op @cur-opacity]
                            (if (< cur-op visible-thresh) (set! (-> cur-mesh .-visible) false)
                                (do
                                  (set! (-> cur-mesh .-visible) true)
                                  (set! (-> cur-mesh .-material .-opacity) @cur-opacity))
                                )
                            
                            )
                          )
                        )

                      )
                    )

             cur-mesh
             )
           )
         )
  )
                      
  
        

(defn scene-init [cur-scene cur-render root1 root2 root3 root4]
  ;(init-cube cur-root)
  ;(init-torus cur-root)
  (init-ply-loader)
  (init-txr-loader)
  (init-lights cur-scene)
  (set! bell-mesh (init-bell root1 (parse-path bell-url) (parse-path bell-sheet)))
  (set! wine-mesh (init-wine root2 (parse-path wine-url) (parse-path wine-sheet)))
  (set! gas-mesh (init-gas root3 (parse-path gas-url) (parse-path gas-sheet)))
  (set! food-mesh (init-food root4 (parse-path food-url) (parse-path food-sheet)))
  ;(set! pic1-mesh (init-pict root1 pic1-url))
  ;(set! pic2-mesh (init-pict root2 pic2-url))
  (swap! update-fns conj (fn [] (update-rms)))
  )
