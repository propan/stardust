(ns stardust.tick
  (:require [stardust.constants :as C]
            [stardust.models :refer [Ship GameScreen]]))

;;
;; Ship Movement Functions
;;
(defn- next-position
  [position dFn velocity max-position]
  (let [next (dFn position velocity)]
    (cond
     (>= next max-position) 0
     (< next 0)             (- max-position 1)
     :default               next)))

(defn- next-rotation
  [rotate rotation turn-factor]
  (case rotate
    :left    (mod (- rotation turn-factor) 360)
    :right   (mod (+ rotation turn-factor) 360)
    rotation))

(defn- next-thrust
  [accelerate thrust]
  (if accelerate
    (min (+ thrust C/ACCELERATION) C/MAX_THRUST)
    (max 0 (- thrust C/THRUST_DECLINE))))

(defn- next-velocity
  [vFn velocity accelerate rotation thrust]
  (if accelerate
    (let [next-velocity (+ velocity (* thrust (vFn (* rotation C/RAD_FACTOR))))]
      (min (max next-velocity (- C/MAX_VELOCITY)) C/MAX_VELOCITY))
    velocity))

;;
;; Tickable Protocol
;;

(defprotocol Tickable
  (tick [_]))

(extend-type Ship
  Tickable
  (tick [{:keys [x y vX vY rotation thrust accelerate rotate shoot ticks-before-shoot immunity] :as ship}]
    (let [shoot? (and shoot (zero? ticks-before-shoot))]
      (merge ship {:x                  (next-position x + vX C/SCREEN_WIDTH)
                   :y                  (next-position y - vY C/SCREEN_HEIGHT)
                   :vX                 (next-velocity Math/sin vX accelerate rotation thrust)
                   :vY                 (next-velocity Math/cos vY accelerate rotation thrust)
                   :rotation           (next-rotation rotate rotation C/TURN_FACTOR)
                   :thrust             (next-thrust accelerate thrust)
                   :ticks-before-shoot (if shoot?
                                         C/TICKS_BETWEEN_SHOOTS
                                         (max 0 (dec ticks-before-shoot)))
                   :immunity           (max 0 (dec immunity))}))))

(extend-type GameScreen
  Tickable
  (tick [{:keys [ship] :as state}]
    (let [{:keys [x y rotation shoot next-shoot]} ship]
      (assoc state :ship (tick ship)))))
