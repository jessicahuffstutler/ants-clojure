(ns ants-clojure.core
  (:require [clojure.java.io :refer [resource]])
  (:import [javafx.application Application]
           [javafx.fxml FXMLLoader]
           [javafx.stage Stage]
           [javafx.scene Scene]
           [javafx.animation AnimationTimer]
           (javafx.scene.paint Color))
  (:gen-class :extends javafx.application.Application))

(def width 800)
(def height 600)
(def ant-count 100)
(def ants (atom nil))                                       ; atom allows us to create a mutable container that we can store things in, here its a list of ants that we are storing.
                                                             ; we can reset the atom everytime we want to move the ants.
                                                             ; global contrainer to hold the ants
(def last-timestamp (atom 0))

(defn create-ants []
  (for [i (range 0 ant-count)]                              ; we are not going to use the i variable but we had to assign it because it wants us to assign a value
    {:x (rand-int width)                                    ; x value here, y value below.
     :y (rand-int height)
     :color Color/GREEN}))

(defn random-step []                                        ; we want this to go left or right, up or down 1 pixel so we need it to return -1 or 1
  (- (* 2 (rand)) 1)                                        ;rand * 2 and then subtract 1 from it
  )

#_(defn distance [[ant] [a]]
  (Math/sqrt
    (+ (Math/pow (- :x ant :x a) 2)
       (Math/pow (- :y ant :y a) 2))))

#_(defn aggravate-ant [ant]
  (let [filter-ants (filter (fn [a]
                              (if (<= (distance [ant] [a]) 10)))
                            (deref ants))]
    (if (> (count filter-ants) 1)
      (assoc ant :color Color/RED)
      (assoc ant :color Color/GREEN))))

(defn aggravate-ant [ant]
  ;let defines a local variable called close-ants
  (let [close-ants (filter (fn [a]                         ;give filter a function, we havec this ant and we need to figure out how many ants are close to it and that's why we need to filter (give a condition and say only give me the ants that match that condition
                              (and (<= (Math/abs (- (:x ant) (:x a))) 10)
                                   (<= (Math/abs (- (:y ant) (:y a))) 10)))
                            (deref ants))]                  ;the collection that we are passing into filter
    (if (= (count close-ants) 1)                            ;if count = 1, return GREEN, if not = 1 then return red.
      (assoc ant :color Color/GREEN)
      (assoc ant :color Color/RED))))

(defn move-ant [ant]                                          ;we only need to write this function to move a single ant
  ;artificially slow this down so we can use parralelism (sleeping for 2 milliseconds) each ant is sleeping 2ms so it's really sleeping 200ms per frame
  ;(Thread/sleep 1)
  ;assoc let's you take a map, like ant
  (assoc ant :x (+ (random-step) (:x ant))                  ;setting new x and y
             :y (+ (random-step) (:y ant))))

(defn draw-ants [context]                                   ; context object (in the argument) will allow us to actually draw things
  ; must clear the frame first before we can draw (below) ; .clearRect is a java method call so it's camel case
  (.clearRect context 0 0 width height)
  ; loop over all the ants and draw it
  ; vector below tells us what to loop over
  (doseq [ant (deref ants)]
    ;set the color of the and and the draw it
    ;imported class Color "javafx.scene.paint Color"
    (.setFill context (:color ant))
    ;5 pixels by 5 pixels, relatively small
    (.fillOval context (:x ant) (:y ant) 5 5)))

(defn fps [now]                                             ;calc the diff btwn now and last timestamp, convert to seconds and then we will do one over that number to get frames per sec
  (let [diff (- now (deref last-timestamp))                 ;we have to do deref because it's inside of the atom, that's how we do mutability
        diff-seconds (/ diff 1000000000)]
    (int (/ 1 diff-seconds))                                ;cast it as an int so we get a single number and no decimal places
    ))

(defn -start [app ^Stage stage]
  (let [root (FXMLLoader/load (resource "main.fxml"))
        scene (Scene. root width height)
        canvas (.lookup scene "#canvas")                    ; control that will be the thing we actually draw on
        fps-label (.lookup scene "#fps")                    ; where we will display frames per sec number
        context (.getGraphicsContext2D canvas)
        timer (proxy [AnimationTimer] []                    ; like creating an anonymous class in java. we want to draw it everytime the canvas is ready to draw something (animation loop)
                (handle [now]
                  (.setText fps-label (str (fps now)))      ;str is converting it into a string
                  (reset! last-timestamp now)
                  ;pmap below boosts performance (that we slowed down with Thread/sleep 2 above) using parallelism.
                  (reset! ants (doall (map aggravate-ant (map move-ant (deref ants))))) ;derefing ants so we are pulling them out of the atom, applying move and that result we are storing in the ants atom and we are doing it before draw ants is called
                  (draw-ants context)))]
    (reset! ants (create-ants))
    (doto stage
      (.setTitle "Ants")
      (.setScene scene)
      (.show))                                              ; creating the window and showing it
    (.start timer)))                                        ; cause the above defn to start running

(defn -main [& args]
  (Application/launch ants_clojure.core (into-array String args)))