package com.example.solarsystem

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.viro.core.*
import com.viro.core.Texture
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.viro.core.Vector
import java.io.IOException
import java.io.InputStream
import java.util.*
import com.viro.core.AmbientLight
import com.viro.core.Material
import com.viro.core.Quad
import com.viro.core.AnimationTransaction


class MainActivity : AppCompatActivity() {

    private val PANORAMA_DEMO = false

    // this is a downscaling trick we have found to reduce some rendering problems inside Viro
    //
    private val VIRO_RENDER_SCALE = 0.01f

    private var viroView: ViroView? = null
    private var camera: Camera? = null
    private var cameraNode: Node? = null
    private var rootNode: Node? = null

    private var scene: Scene? = null
    private var obj3d: Object3D? = null

    private var orbitRadius = 10f
    private var cameraPitch = Math.PI / 2.toFloat()
    private var cameraYaw = 0f

    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()

    private var planetNode: Node? = null
    private var boxNode: Node? = null
    private var planetNames = arrayOf("mercury", "venus", "earth", "mars", "jupiter", "saturn", "uranus", "neptune", "pluto")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        val config = RendererConfiguration()
        config.isPBREnabled = false
        config.isHDREnabled = false
        config.isBloomEnabled = false
        config.isShadowsEnabled = false

        viroView = ViroViewScene(this, object : ViroViewScene.StartupListener {
            override fun onSuccess() {
                //displayScene()

                if(PANORAMA_DEMO)
                    load360Background()
                else
                    load3dObject()

                // Display the scene
                viroView!!.scene = scene

                //create touch
                createTouchListener()

            }

            override fun onFailure(error: ViroViewScene.StartupError, errorMessage: String) {
                Log.e(this@MainActivity::class.qualifiedName, "Error initializing AR [$errorMessage]")
            }
        }, config)

        viroView!!.setCameraListener{ position: Vector, rotation: Vector, zoom: Vector ->
            //Toast.makeText(this@MainActivity, rotation.toString(), Toast.LENGTH_SHORT).show()
            //Log.d("###", "%.2f".format(rotation.y))
        }

        setContentView(viroView)
    }

    fun load360Background(){
        scene = Scene()
        rootNode = scene!!.rootNode

        viroView!!.setVRModeEnabled(false)

        var bitmap = getBitmapFromAssets("universe_360.jpeg")
        var texture = Texture(bitmap, Texture.Format.RGBA8, true, true)

        scene!!.setBackgroundTexture(texture)

        setupCamera()

        loadPlanets()

        //another way to load 360 image
        /*val obj3d = Object3D()
        obj3d.loadModel(viroView!!.viroContext,
            Uri.parse("file:///android_asset/sphere.obj"),
            Object3D.Type.OBJ, object : AsyncObject3DListener{
                override fun onObject3DLoaded(obj: Object3D?, objType: Object3D.Type?) {
                    val bitmap = getBitmapFromAssets("theatre_360.jpg")
                    applyBackgroundToSphere(obj!!, bitmap!!)

                    setupCamera()
                }

                override fun onObject3DFailed(p0: String?) {
                    Log.i("OBJECT3D", p0.toString())
                }
            })

        rootNode!!.addChildNode(obj3d)*/

    }

    private fun loadPlanets(){

        var planetPosition = 0
        while (planetPosition <= 4){
            // get basic android layout
            val earthBitmap = getLayout(planetPosition)

            var planetNodePosition: Vector? = null
            var boxNodePosition: Vector? = null
            var planetScale: Vector? = null
            when(planetPosition){
                0 -> {
                    planetNodePosition = Vector(0f, 0f, 3f)
                    boxNodePosition = Vector(0f, 0f, 2.95f)
                    planetScale = Vector(2f, 2f, 2f)
                }
                1 -> {
                    planetNodePosition = Vector(1f, 0f, -3f)
                    boxNodePosition = Vector(1f, 0f, -2.95f)
                    planetScale = Vector(2f, 2f, 2f)
                }
                2 -> {
                    planetNodePosition = Vector(0f, 0f, -4f)
                    boxNodePosition = Vector(0f, 0f, -3.95f)
                    planetScale = Vector(2f, 2f, 2f)
                }
                3 -> {
                    planetNodePosition = Vector(3f, 0f, 0f)
                    boxNodePosition = Vector(2.95f, 0f, 0f)
                    planetScale = Vector(2f, 2f, 2f)
                }
                4 -> {
                    planetNodePosition = Vector(-3f, 0f, 0f)
                    boxNodePosition = Vector(-2.95f, 0f, 0f)
                    planetScale = Vector(2f, 2f, 2f)
                }
            }
            setupPlanetTexture(earthBitmap, planetPosition, planetNodePosition!!, boxNodePosition!!, planetScale!!)
            planetPosition++
        }

    }

    private fun setupPlanetTexture(bitmap: Bitmap, planetPosition: Int, planetNodePosition: Vector, boxNodePosition: Vector, planetScale: Vector){
        val texture = Texture(bitmap, Texture.Format.RGBA8, true, true)

        // Set the Texture to be used on our surface in 3D.
        val material = Material()
        material.diffuseTexture = texture

        val surface = Quad(1f, 1f)
        surface.materials = Arrays.asList(material)

        setupPlanetNode(surface, planetPosition, planetNodePosition, planetScale, boxNodePosition)

    }

    private fun setupPlanetNode(surface: Quad, planetPosition: Int, planetNodePosition: Vector, planetScale: Vector, boxNodePosition: Vector){
        planetNode = Node()
        planetNode!!.setPosition(planetNodePosition)
        planetNode!!.setScale(planetScale)
        planetNode!!.geometry = surface

        //set name for planet node
        planetNode!!.name = planetNames[planetPosition]

        //rotate planet facing the camera
        planetNode!!.transformBehaviors = EnumSet.of(Node.TransformBehavior.BILLBOARD_Y)
        Log.d("###", "rotation: ${planetNode!!.rotationEulerRealtime} | planetNodePosition: $planetNodePosition | position RT: ${planetNode!!.positionRealtime} | name: ${planetNode!!.name}")

        //setup 3D cubes/boxes for handling clicks
        boxNode = createBoxNode()
        boxNode!!.setPosition(boxNodePosition)

        //set name for box node
        boxNode!!.name = planetNames[planetPosition]

        //set click listener
        boxNode!!.clickListener = NodeOnClickListener()

        //add node to scene
        scene!!.rootNode.addChildNode(planetNode)
        scene!!.rootNode.addChildNode(boxNode)

    }

    private fun createBoxNode(): Node {
        boxNode = Node()

        val box = Box(1f, 1f, 1f)

        val material = Material.MaterialBuilder()
            .diffuseColor(Color.TRANSPARENT)
            .transparencyMode(Material.TransparencyMode.A_ONE)
            .build()

        box.materials = Arrays.asList(material)

        boxNode!!.geometry = box

        boxNode!!.tag = planetNode!!.name + "box"

        return boxNode as Node
    }

    private fun getLayout(position: Int): Bitmap{
        //load views from layout
        val layoutHotspot = layoutInflater.inflate(R.layout.hotspot_planet, null)
        //val title = layoutHotspot.findViewById<TextView>(R.id.txt_title)
        val img = layoutHotspot.findViewById<ImageView>(R.id.img)
        when(position){
            0 -> img.setBackgroundResource(R.drawable.mercury)
            1 -> img.setBackgroundResource(R.drawable.venus)
            2 -> img.setBackgroundResource(R.drawable.earth)
            3 -> img.setBackgroundResource(R.drawable.mars)
            4 -> img.setBackgroundResource(R.drawable.jupiter)
        }

        return loadBitmapFromView(layoutHotspot)
    }

    private fun loadBitmapFromView(v: View): Bitmap {
        if (v.measuredHeight <= 0) {
            val specWidth = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            v.measure(specWidth, specWidth)
            val b = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            v.layout(0, 0, v.measuredWidth, v.measuredHeight)
            v.draw(c)
            return b
        }

        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    private fun load3dObject() {

        // Creation of SceneControllerJni within scene navigator
        scene = Scene()
        rootNode = scene!!.rootNode

        obj3d = Object3D()

        viroView!!.setVRModeEnabled(false)


        obj3d!!.loadModel(viroView!!.viroContext,
            Uri.parse("file:///android_asset/spaceship.vrx"),
            Object3D.Type.FBX, object : AsyncObject3DListener {
                override fun onObject3DLoaded(object3D: Object3D, type: Object3D.Type) {
                    object3D.setRotation(Vector(Math.PI / 2, 0.0, (Math.PI / 2) * 2))

                    setupLights()
                    setupCamera()
                }

                override fun onObject3DFailed(error: String) {
                    Log.i("xxx", "error: $error")
                }
            })

        rootNode!!.addChildNode(obj3d)

        obj3d!!.setScale(Vector(VIRO_RENDER_SCALE, VIRO_RENDER_SCALE, VIRO_RENDER_SCALE))
        obj3d!!.clickListener = NodeOnClickListener()

    }

    private fun setupCamera() {

        cameraNode = Node()
        camera = Camera()
        cameraNode!!.camera = camera
        rootNode!!.addChildNode(cameraNode)
        viroView!!.setPointOfView(cameraNode)

        if (PANORAMA_DEMO == false) {
            orbitRadius = 10f
            setCameraPositionAndRotation()
        }

    }

    private fun setupLights() {

        // this is the Omni light code that we'd like to use, but it's showing as black
        /*val lightD = 1000f
        val dLightNode1 = Node()
        val dLight1 = OmniLight()
        dLight1.intensity = 1000f
        dLightNode1.addLight(dLight1)
        dLightNode1.setPosition(Vector(lightD / 3, lightD, lightD / 3))
        scene!!.rootNode.addChildNode(dLightNode1)

        val dLightNode2 = Node()
        val dLight2 = OmniLight()
        dLight2.intensity = 1000f
        dLightNode2.addLight(dLight2)
        dLightNode2.setPosition(Vector(-lightD / 3, -lightD, -lightD / 3))
        scene!!.rootNode.addChildNode(dLightNode2)

        // make sure attenuation is not applied to the scene at all
        dLight1.attenuationStartDistance = lightD * 3
        dLight1.attenuationEndDistance = lightD * 9

        dLight2.attenuationStartDistance = lightD * 3
        dLight2.attenuationEndDistance = lightD * 9*/

        val ambient = AmbientLight(Color.WHITE.toLong(), 1000.0f)
        rootNode!!.addLight(ambient)
    }

    private fun applyBackgroundToSphere(sphereNode: Node, bitmap: Bitmap) {

        val texture = Texture(bitmap, Texture.Format.RGBA8, true, true)

        // this prevents the streaking at the N/S poles (instead of LINEAR)
        texture.magnificationFilter = Texture.FilterMode.NEAREST
        texture.minificationFilter = Texture.FilterMode.NEAREST

        val material = Material()
        material.diffuseTexture = texture
        material.cullMode = Material.CullMode.BACK

        // apply texture to the sphere
        //
        sphereNode.geometry.materials = Arrays.asList(material)
    }

    private fun setCameraPositionAndRotation() {

        if (PANORAMA_DEMO) {
            if (cameraNode != null) {

                val angles = Vector(cameraPitch - Math.PI / 2.0, cameraYaw.toDouble(), 0.0)
                val r = Quaternion(angles)
                cameraNode!!.setRotation(r)
            }
        } else {


            val camZ = orbitRadius.toDouble() * Math.cos(cameraYaw.toDouble()) * Math.sin(cameraPitch)
            val camX = orbitRadius.toDouble() * Math.sin(cameraYaw.toDouble()) * Math.sin(cameraPitch)
            val camY = orbitRadius * Math.cos(cameraPitch)

            if (cameraNode != null) {

                cameraNode!!.setPosition(Vector(camX, camY, camZ).scale(VIRO_RENDER_SCALE))

                // rotate camera to face 0,0,0
                val angles = Vector(cameraPitch - Math.PI / 2.0, cameraYaw.toDouble(), 0.0)
                val r = Quaternion(angles)
                cameraNode!!.setRotation(r)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createTouchListener() {

        viroView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {


                when (motionEvent.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = motionEvent.getX(0)
                        lastTouchY = motionEvent.getY(0)
                    }


                    MotionEvent.ACTION_UP -> {
                    }

                    MotionEvent.ACTION_MOVE -> {
                        var dx = motionEvent.x - lastTouchX
                        var dy = motionEvent.y - lastTouchY
                        if (Math.abs(dx) > 100 || Math.abs(dy) > 100) {
                            dy = 0f
                            dx = dy
                        }
                        setCameraPositionAndRotation()

                        if (PANORAMA_DEMO) {
                            cameraYaw += 3 * dx / viroView!!.width
                            cameraPitch += 3 * dy / viroView!!.height
                        } else {
                            cameraYaw -= 3 * dx / viroView!!.width
                            cameraPitch -= 3 * dy / viroView!!.height
                        }

                        lastTouchX = motionEvent.x
                        lastTouchY = motionEvent.y
                    }

                }

                return false
            }
        })
    }

    private fun getBitmapFromAssets(assetName: String): Bitmap? {
        val assetManager = resources.assets
        val input: InputStream
        try {
            input = assetManager.open(assetName)
        } catch (e: IOException) {
            e.stackTrace
            Log.w(this@MainActivity::class.qualifiedName, "Unable to find image [" + assetName + "] in assets! Error: " + e.message)
            return null
        }

        return BitmapFactory.decodeStream(input)
    }

    inner class NodeOnClickListener: ClickListener{
        override fun onClick(p0: Int, p1: Node?, p2: Vector?) {
            if (PANORAMA_DEMO == false) {
                Log.d("###", "camera position before: ${cameraNode!!.positionRealtime}")
                moveCameraAndRotate(Vector(2f, 2f, 2f), Vector(0f, 0f, 0f))
            }else{
                Toast.makeText(this@MainActivity, p1!!.name, Toast.LENGTH_LONG).show()
                Log.d("###", "camera position: ${camera!!.position} | planet: ${p1.name} | planet rotation: ${p1.rotationQuaternionRealtime}")
            }
        }

        override fun onClickState(p0: Int, p1: Node?, p2: ClickState?, p3: Vector?) {
            //Log.d("###", "click state: ${p2.toString()}")
        }

    }

    /**
     * move camera to new position and adjust rotation accordingly
     */
    private fun moveCameraAndRotate(cameraNewPosition: Vector, lookAtPosition: Vector){
        // Grab new forward vectors facing the lookAtPosition for our Camera
        val cameraLastForward = viroView!!.lastCameraForwardRealtime
        val cameraNewForward = lookAtPosition.subtract(cameraNewPosition.scale(VIRO_RENDER_SCALE))

        // Calculate and apply the needed rotation delta to be applied to our Camera.
        val rotationDelta = Quaternion.makeRotationFromTo(cameraLastForward.normalize(), cameraNewForward.normalize())
        val newRot = rotationDelta.toEuler().add(viroView!!.lastCameraRotationEulerRealtime)

        // Animate the Camera to the desired point (optional, you can also just set the position).
        AnimationTransaction.begin()
        AnimationTransaction.setAnimationDuration(2000)
        AnimationTransaction.setTimingFunction(AnimationTimingFunction.Linear)
        AnimationTransaction.setListener(CameraAnimationListener())
        cameraNode!!.setPosition(cameraNewPosition.scale(VIRO_RENDER_SCALE))
        cameraNode!!.setRotation(Quaternion(newRot))
        AnimationTransaction.commit()
    }

    inner class CameraAnimationListener: AnimationTransaction.Listener{
        override fun onFinish(p0: AnimationTransaction?) {
            Log.d("###", "camera position after : ${cameraNode!!.positionRealtime}")
        }
    }
}
