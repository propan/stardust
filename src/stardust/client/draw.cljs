(ns stardust.client.draw
  (:require-macros [stardust.client.macros :refer [with-context]])
  (:require [stardust.constants :as C]
            [stardust.client.constants :as CC]
            [stardust.models :refer [Bullet ObjectPiece Player Ship ConnectionScreen DeathMatchScreen]]))

;;
;; Helpers
;;

(defn- create-gradient
  [ctx x y radius color]
  (doto (.createRadialGradient ctx x y 0 x y radius)
    (.addColorStop 0 "#FFFFFF")
    (.addColorStop 0.4 "#FFFFFF")
    (.addColorStop 0.4 color)
    (.addColorStop 1.0 "#000000")))

(defn- fill-text
  [context text x y font color]
  (with-context [ctx context]
    (doto ctx
      (aset "font" font)
      (aset "fillStyle" color)
      (.fillText text x y))))

(defn- fill-text-centered
  [context text x y font color]
  (with-context [ctx context]
    (doto ctx
      (aset "font" font)
      (aset "fillStyle" color)
      (.fillText text (- x (/ (.-width (.measureText ctx text)) 2)) y))))

(defn- draw-cached-image
  [context image x y rotation]
  (with-context [ctx context]
    (doto ctx
      (.translate x y)
      (.rotate (* rotation C/RAD_FACTOR))
      (.drawImage (.-data image) (.-offset-x image) (.-offset-y image)))))

(defn- draw-path
  [context path]
  (when-let [[x y] (first path)]
    (.beginPath context)
    (.moveTo context x y)
    (doseq [[x y] (rest path)]
      (.lineTo context x y))
    (.closePath context)))

(defn- draw-shield
  [context immunity]
  (when (pos? immunity)
    (with-context [ctx context]
      (doto ctx
        (aset "strokeStyle" CC/SHIELD_COLOR)
        (aset "shadowBlur"  CC/SHADOW_BLUR)
        (aset "shadowColor" CC/SHIELD_COLOR)
        (aset "lineWidth" 1.5)
        (aset "globalAlpha" (/ immunity C/SPAWN_IMMUNITY_SECONDS))
        (.beginPath)
        (.arc 0 0 CC/SHIP_SHIELD_RADIUS (* 2 Math/PI) false)
        (.closePath)
        (.stroke)))))

;;
;; Pre-generated images
;;

(deftype CachedImage [offset-x offset-y data])

(defn- generate-ship-image
  [buffer color]
  (let [image-width  (+ 20 (* 2 CC/SHADOW_BLUR))
        image-height (+ 25 (* 2 CC/SHADOW_BLUR))
        middle-x     (/ image-width 2)
        middle-y     (/ image-width 2)
        image        (js/Image.)]
    (set! (.-width buffer) image-width)
    (set! (.-height buffer) image-height)
    (with-context [ctx (.getContext buffer "2d")]
      (doto ctx
        (aset "shadowBlur" CC/SHADOW_BLUR)
        (aset "shadowColor" color)
        (aset "strokeStyle" color)
        (aset "lineWidth" 2.5)
        (.translate middle-x middle-y)
        (.beginPath)
        (.moveTo -10 10)
        (.lineTo 0 -15)
        (.lineTo 10 10)
        (.moveTo 7 5)
        (.lineTo -7 5)
        (.closePath)
        (.stroke)))
    (set! (.-src image) (.toDataURL buffer "image/png"))
    (CachedImage. (- middle-x) (- middle-y) image)))

(defn generate-particle-image
  [buffer radius color]
  (let [image-size (* 2 (+ radius CC/SHADOW_BLUR))
        middle     (/ image-size 2)
        image      (js/Image.)]
    ;; resize buffer
    (set! (.-width buffer) image-size)
    (set! (.-height buffer) image-size)
    (with-context [ctx (.getContext buffer "2d")]
      (doto ctx
        (aset "globalCompositeOperation" "lighter")
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" radius)
        (aset "fillStyle" (create-gradient ctx 0 0 radius color))
        (.translate middle middle)
        (.beginPath)
        (.arc 0 0 radius (* 2 Math/PI) false)
        (.fill)))
    (set! (.-src image) (.toDataURL buffer "image/png"))
    (CachedImage. (- middle) (- middle) image)))

;;
;; Cached images
;;

(def ship-images
  (let [buffer (.createElement js/document "canvas")]
    (into-array (for [color (range 0 6)]
                  (generate-ship-image buffer (get CC/SHIP_COLORS color))))))

(def bullet-image
  (generate-particle-image (.createElement js/document "canvas") CC/BULLET_RADIUS CC/BULLET_COLOR))
;;
;;
;;

(defn- draw-ship
  [context x y rotation immunity color]
  (with-context [ctx context]
    (doto ctx
      (draw-cached-image (aget ship-images color) x y rotation)
      (.translate x y)
      (draw-shield immunity))))

;;
;; Drawable Protocol
;;

(defprotocol Drawable
  (draw [_ context]))

(extend-type Bullet
  Drawable
  (draw [{:keys [x y]} context]
    (draw-cached-image context bullet-image x y 0)))

(extend-type ObjectPiece
  Drawable
  (draw [{:keys [x y h size rotation color path]} context]
    (with-context [ctx context]
      (let [scale (* 1.5 size)
            width (* 1.0 (/ 1.5 size))
            color (get CC/SHIP_COLORS color)]
        (doto ctx
          (aset "shadowBlur" 15)
          (aset "shadowColor" color)
          (aset "lineWidth" width)
          (aset "strokeStyle" color)
          (.translate x y)
          (.scale scale scale)
          (.rotate (* rotation C/RAD_FACTOR))
          (draw-path path)
          (.stroke))))))

(extend-type Ship
  Drawable
  (draw [{:keys [x y h immunity color]} context]
    (draw-ship context x y h immunity color)))

(extend-type Player
  Drawable
  (draw [{:keys [x y h immunity color]} context]
    (draw-ship context x y h immunity color)))

(defn connecting-string
  []
  (let [times (mod (.getSeconds (js/Date.)) 4)]
    (apply str (cons "Connecting" (repeat times ".")))))

(extend-type ConnectionScreen
  Drawable
  (draw [{:keys [fps] :as state} context]
    (doto context
      (.clearRect 0 0 CC/SCREEN_WIDTH CC/SCREEN_HEIGHT)
      (fill-text (str fps " FPS") 10 20 "14px Helvetica" "#FFFFFF")
      (fill-text (connecting-string) (- (/ CC/SCREEN_WIDTH 2) 110) (/ CC/SCREEN_HEIGHT 2) "34px Rammetto One" "#FFFFFF"))))

(extend-type DeathMatchScreen
  Drawable
  (draw [{:keys [player fps ships effects bullets] :as state} context]
    (.clearRect context 0 0 CC/SCREEN_WIDTH CC/SCREEN_HEIGHT)
    (fill-text context (str fps " FPS") 10 20 "14px Helvetica" "#FFFFFF")
    (doseq [bullet bullets]
      (draw bullet context))
    (draw player context)
    (doseq [ship ships]
      (draw ship context))
    (doseq [effect effects]
      (draw effect context))))
