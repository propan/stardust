(ns stardust.handlers
  (:require [stardust.models :refer [GameScreen]]))

(defprotocol Handler
  (handle [_ event]))

(extend-type GameScreen
  Handler
  (handle [state event]
    state))
