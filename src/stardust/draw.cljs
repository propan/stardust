(ns stardust.draw
  (:require-macros [stardust.macros :refer [with-context]])
  (:require [stardust.constants :as C]
            [stardust.models :refer [CachedImage Ship GameScreen]]))

;;
;; Helpers
;;

(defn- fill-text
  [context text x y font color]
  (with-context [ctx context]
    (doto ctx
      (aset "font" font)
      (aset "fillStyle" color)
      (.fillText text x y))))

(defn- draw-cached-image
  [context image x y rotation]
  (with-context [ctx context]
    (doto ctx
      (.translate x y)
      (.rotate (* rotation C/RAD_FACTOR))
      (.drawImage (.-data image) (.-offset-x image) (.-offset-y image)))))

;;
;; Pre-generated images
;;

(defn- generate-ship-image
  [buffer color]
  (let [image-width  (+ 20 (* 2 C/SHADOW_BLUR))
        image-height (+ 25 (* 2 C/SHADOW_BLUR))
        middle-x     (/ image-width 2)
        middle-y     (/ image-width 2)
        image        (js/Image.)]
    (set! (.-width buffer) image-width)
    (set! (.-height buffer) image-height)
    (with-context [ctx (.getContext buffer "2d")]
      (doto ctx
        (aset "shadowBlur" C/SHADOW_BLUR)
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

;;
;; Cached images
;;

(def ship-images
  (let [buffer (.createElement js/document "canvas")]
    (into-array (for [type (range 0 6)]
                  (generate-ship-image buffer (get C/SHIP_COLORS type))))))

;;
;; Drawable Protocol
;;

(defprotocol Drawable
  (draw [_ context]))

(extend-type Ship
  Drawable
  (draw [{:keys [x y rotation immunity type]} context]
    (with-context [ctx context]
      (draw-cached-image ctx (aget ship-images type) x y rotation))))

(extend-type GameScreen
  Drawable
  (draw [{:keys [width height ship fps]} context]
    (.clearRect context 0 0 width height)
    (fill-text context (str fps " FPS") 10 20 "14px Helvetica" "#FFFFFF")
    (draw ship context)))
