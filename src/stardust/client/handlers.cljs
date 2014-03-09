(ns stardust.client.handlers
  (:require [clojure.browser.dom :as dom]
            [stardust.client.draw :as d]
            [stardust.models :refer [DeathMatchScreen]]))

(def context (.getContext (dom/get-element "open-space") "2d"))

(defn- change-ship-state
  [state property from to]
  (update-in state [:player property] #(if (= % from) to %)))

(defn- handle-frame
  [state fps]
  (d/draw state context)
  (assoc state :fps fps))

(defn- gs-handle-keyboard
  [state event]
  (case event
    :arrow-left-down  (change-ship-state state :rotate :none :left)
    :arrow-left-up    (change-ship-state state :rotate :left :none)
    :arrow-right-down (change-ship-state state :rotate :none :right)
    :arrow-right-up   (change-ship-state state :rotate :right :none)
    :arrow-up-down    (change-ship-state state :accelerate false true)
    :arrow-up-up      (change-ship-state state :accelerate true false)
    state))

;;
;; Handler Protocol
;;

(defprotocol Handler
  (handle [_ event]))

(extend-type DeathMatchScreen
  Handler
  (handle [state [source data]]
    (case source
      :frame    (handle-frame state data)
      :keyboard (gs-handle-keyboard state data)
      state)))
