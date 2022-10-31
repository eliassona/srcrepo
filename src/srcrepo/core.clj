(ns srcrepo.core
  (:import [java.security MessageDigest]
           [java.io FileInputStream FileOutputStream File]
           [redis.clients.jedis Jedis]
           [srcrepo Codec]))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def sha-256 (MessageDigest/getInstance "SHA-256"))


(defn file-name->ba [file-name]
  (let [in-stream (FileInputStream. file-name)
        ba (byte-array (.available in-stream))
        _ (.read in-stream ba)]
    ba))

(defn ba->ba-str [ba]
  (apply str (map (partial format "%02x") ba)))

(defn file->sha-256 [file]
  (.digest sha-256 (file-name->ba file)))

(defn add-file-to-repo [src-file bin-files]
   (let [k (file->sha-256 src-file)]
     {k (map (fn [f] [(.getName f) (file-name->ba f)]) bin-files)}))

(defn files-of [dir ext]
  (let [ext (format ".%s" ext)]
    (filter (fn [path] (.endsWith (.getName path) ext)) (file-seq (clojure.java.io/file dir)))))

(defn relative-path-of [root-dir file]
  (.substring (.getAbsolutePath file) (-> root-dir .getAbsolutePath count inc)))

(defn pair-src-file [src-dir bin-dir bin-files]
  (let [relative-bin-files (map (partial relative-path-of bin-dir) bin-files)]
    (fn [src-file] 
      (let [relative-src-file (relative-path-of src-dir src-file)
            relative-src-file-no-ext (.substring relative-src-file 0 (- (count relative-src-file) 5))]
        [(File. src-dir relative-src-file) 
         (map (fn [rf] (File. bin-dir rf)) (filter #(.startsWith % relative-src-file-no-ext) relative-bin-files))]
  ))))

(defn pair-src-bin-files [[src-dir src-ext] [bin-dir bin-ext]]
  (if (and (.isDirectory src-dir) (.isDirectory bin-dir))
   (let [src-files (files-of src-dir src-ext)
         bin-files (files-of bin-dir bin-ext)
         pair-src-file-fn (pair-src-file src-dir bin-dir bin-files)]
     (map pair-src-file-fn src-files))
  (throw (IllegalArgumentException. "Must be directory"))
  ))


(defn add-dir-to-repo [src bin]
  (apply merge (map (fn [[src-file bin-files]] (add-file-to-repo src-file bin-files)) (pair-src-bin-files src bin)))
  )

(def jedis (Jedis. "localhost" 6379)) 

(defn add-dir-to-redis! [src bin]
  (doseq [[k v] (add-dir-to-repo src bin)]
    (.set jedis k (Codec/encodeBA v))))

(defn remove-name [path]
  (.getParent (File. path)))

(defn change-if-scala [bin-dir]
  (if (.exists bin-dir)
    bin-dir
    (let [p (.getAbsolutePath bin-dir)]
      (File. (.replaceFirst p "/java" "/scala")))))

(defn init-bin! [[src-dir src-ext] bin-root-dir]
  (let [bin-root-dir (change-if-scala bin-root-dir)
        src-files 
        (map (fn [f] [(remove-name (relative-path-of src-dir f)) (.get jedis (file->sha-256 f))]) (files-of src-dir src-ext))
        src-files (filter (fn [[_ ba]] ba) src-files)]
    (doseq [[rel-src-dir bin-files] src-files]
      (let [bin-dir (File. bin-root-dir rel-src-dir)]
        (.mkdirs bin-dir)
        (doseq [bin-file (Codec/decodeBA bin-files)]
          (let [[bin-name ba] bin-file
                f (File. bin-dir bin-name)
                out (FileOutputStream. f)]
            (try 
              (.write out ba)
              (.flush out)
              (finally
                (.close out)))))))))



(def ultra-project 
  [[[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/external/java") "java"]
    [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes/java/external") "class"]]
  [[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/devkit/java") "java"]
   [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes/java/devkit") "class"]]
   [[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/server/java") "java"]
    [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes/java/server") "class"]]
   [[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/testsupport/java") "java"]
    [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes/java/testsupport") "class"]]
   #_[[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/ui/java") "java"]
     [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes/java/ui") "class"]]])
(def http-project 
  [[[(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/http/src/main/java") "java"]
    [(File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/http/build/classes/java/main") "class"]]
   ])  
(defn add! [] 
  (doseq [[src bin] http-project]
    (add-dir-to-redis! src bin))
  (println (str (count (.keys jedis "*")) " object created in redis")))


(defn init! []
  (doseq [[src [bin-dir]] http-project]
    (init-bin! src bin-dir)))


(comment
  (let [f (File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src/ui/java/com/digitalroute/wfc/ultra/ui/udr")
      java-files (filter #(.endsWith (.getName %) ".java") (.listFiles f))]
  (reduce + (map (comp count #(clojure.string/split % #"\n") slurp) java-files)))
  )
  
  