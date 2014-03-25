(ns stardust.client.handlers
  (:require [clojure.browser.dom :as dom]
            [cljs.core.async :refer [put!]]
            [stardust.client.draw :as d]
            [stardust.models :as m :refer [ConnectionScreen DeathMatchScreen]]
            [stardust.protocols :as p :refer [Handler]]
            [stardust.tick]
            [stardust.utils :as u]))

(def context (.getContext (dom/get-element "open-space") "2d"))

(defn- change-ship-state
  [state property from to]
  (update-in state [:player property] #(if (= % from) to %)))

(defn- handle-frame
  [state between]
  (d/draw state context)
  (-> state
      (assoc :fps (u/round (/ 1000 between)))
      (p/tick (/ between 1000.0))))

(defn- dms-handle-keyboard
  [state event]
  (put! (:out-channel state) event)
  state)

(defn- merge-state
  [{:keys [out-channel fps effects]} data]
  (merge data {:fps         fps
               :out-channel out-channel
               :effects     (or effects [])}))

(defn- player-join
  [state {:keys [client-id] :as player}]
  (-> state
      (assoc-in  [:players client-id] player)
      (assoc-in  [:score client-id] 0)))

(defn- player-leave
  [state client-id]
  (-> state
      (update-in [:players] dissoc client-id)
      (update-in [:score]   dissoc client-id)))

(defn- update-player
  [state {:keys [client-id] :as player}]
  (assoc-in state [:players client-id] player))

(defn- handle-socket-message
  [state [source data]]
  (case source
    :state    (merge-state   state data)
    :join     (player-join   state data) ;; TODO: animate
    :leave    (player-leave  state data) ;; TODO: animate
    :spawn    (update-player state data) ;; TODO: animate ship crash + respawn
    :player   (update-player state data)
    state))

(defn handle-socket
  [state [event data]]
  (case event
    :message (handle-socket-message state data)
    :closed  (m/connection-screen (:out-channel state))
    state))

(extend-type ConnectionScreen
  Handler
  (handle [state event]
    (let [[source data] event]
      (case source
        :frame    (handle-frame state data)
        :socket   (handle-socket state data)
        state))))

(extend-type DeathMatchScreen
  Handler
  (handle [state event]
    (let [[source data] event]
      (case source
        :frame    (handle-frame state data)
        :keyboard (dms-handle-keyboard state event)
        :socket   (handle-socket state data)
        state))))
