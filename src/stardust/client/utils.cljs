(ns stardust.client.utils)

(defn- raf-fn
  [last-time callback element]
  (let [current-time (.getTime (js/Date.))
        timeout      (Math/max 0 (- 16 (- current-time last-time)))
        next-call    (+ current-time timeout)]
    (.setTimeout js/window #(callback next-call) timeout)
    (aset js/window "requestAnimationFrame" (partial raf-fn next-call))))

(defn init-request-animation-frame
  "Based on http://paulirish.com/2011/requestanimationframe-for-smart-animating"
  []
  (let [last-time 0]
    ;; resolve vendors rAF 
    (loop [afn     (.-requestAnimationFrame js/window)
           vendors ["ms" "webkit" "moz" "o"]]
      (when-not (or (not (nil? afn))
                    (empty? vendors))
        (let [vendor (first vendors)
              afn    (aget js/window (str vendor "RequestAnimationFrame"))]
          (println vendor)
          (aset js/window "requestAnimationFrame" afn)
          (aset js/window "cancelAnimationFrame" (or (aget js/window (str vendor "CancelAnimationFrame"))
                                                     (aget js/window (str vendor "CancelRequestAnimationFrame"))))
          (recur afn (rest vendors)))))
    ;; improvise, if it's not resolved
    (when-not (.-requestAnimationFrame js/window)
      (aset js/window "requestAnimationFrame" (partial raf-fn 0)))
    (when-not (.-cancelAnimationFrame js/window)
      (aset js/window "cancelAnimationFrame" js/clearTimeout))))
