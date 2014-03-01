(ns stardust.constants)

(def RAD_FACTOR (/ Math.PI 180))

;;
;; Screen Dimensions
;;

(def SCREEN_WIDTH 1000)
(def SCREEN_HEIGHT 600)

(def RIGHT_EDGE (- SCREEN_WIDTH 5))
(def BOTTOM_EDGE (- SCREEN_HEIGHT 5))

;;
;; Movement
;;

(def MAX_VELOCITY 6)
(def MAX_THRUST 2)

(def THRUST_DECLINE 0.3)

(def TURN_FACTOR 4)
(def ACCELERATION 0.01)

;;
;; Shooting
;;

(def TICKS_BETWEEN_SHOOTS 20)

;;
;; Graphics
;;

(def SHADOW_BLUR 10)

(def SHIP_COLORS ["#00CBE7" "#FF8A00" "#FABE28" "#88C100" "#FF003C"])
