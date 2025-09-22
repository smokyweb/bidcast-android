package io.bidswipe.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.bidswipe.app.databinding.ActivitySpoofSocketBinding
import io.bidswipe.app.model.LiveSocketModel
import io.bidswipe.app.utils.SocketManager

class SpoofSocketActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpoofSocketBinding
    private lateinit var socketManager: SocketManager

    private val serverUrl = "https://node.bidcast.betaplanets.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpoofSocketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSocketManager()
        setupClickListeners()
    }

    private fun setupSocketManager() {
        socketManager = SocketManager.getInstance(this)

        // Initialize socket with server URL
        socketManager.initialize(serverUrl)

        // Set up socket event listeners
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        // Listen for bid updates
        socketManager.onBidUpdate { bidJson ->
            runOnUiThread {
                val message = "Bid Update: ${bidJson.toString()}"
                appendMessage(message)
            }
        }

        // Listen for viewer count updates
        socketManager.onViewerCount { count ->
            runOnUiThread {
                val message = "Viewer Count: $count"
                appendMessage(message)
            }
        }

        // Listen for general messages
        socketManager.onMessage { messageJson ->
            runOnUiThread {
                val message = "Message: ${messageJson.toString()}"
                appendMessage(message)
            }
        }

        // Listen for live socket updates
        socketManager.onLiveSocketUpdate { liveSocket ->
            runOnUiThread {
                val message =
                    "Live Socket Update: RoomId=${liveSocket.roomId}, ShowId=${liveSocket.showId}, ViewerCount=${liveSocket.viewerCount}, IsLive=${liveSocket.isLive}"
                appendMessage(message)
            }
        }

        // Listen for product status updates
        socketManager.onProductStatusUpdate { productJson ->
            runOnUiThread {
                val message = "Product Status Update: ${productJson.toString()}"
                appendMessage(message)
            }
        }

        // Listen for current product changes
        socketManager.onCurrentProductChange { productJson ->
            runOnUiThread {
                val message = "Current Product Change: ${productJson.toString()}"
                appendMessage(message)
            }
        }
    }

    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            connectToSocket()
        }

        binding.disconnectButton.setOnClickListener {
            disconnectFromSocket()
        }

        binding.joinRoomButton.setOnClickListener {
            val roomId = binding.roomIdEditText.text.toString().trim()
            if (roomId.isNotEmpty()) {
                joinRoom(roomId)
            } else {
                Toast.makeText(this, "Please enter a room ID", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val roomId = binding.roomIdEditText.text.toString().trim()
            val message = binding.messageEditText.text.toString().trim()
            if (roomId.isNotEmpty() && message.isNotEmpty()) {
                sendMessage(roomId, message)
            } else {
                Toast.makeText(this, "Please enter room ID and message", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sendBidButton.setOnClickListener {
            val roomId = binding.roomIdEditText.text.toString().trim()
            if (roomId.isNotEmpty()) {
                sendTestBid(roomId)
            } else {
                Toast.makeText(this, "Please enter a room ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToSocket() {
        updateStatus("Connecting...")

        socketManager.connect(
            onConnected = {
                runOnUiThread {
                    updateStatus("Connected to $serverUrl")
                    Toast.makeText(this, "Connected successfully!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    updateStatus("Connection failed: $error")
                    Toast.makeText(this, "Connection failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun disconnectFromSocket() {
        socketManager.disconnect()
        updateStatus("Disconnected")
        Toast.makeText(this, "Disconnected from socket", Toast.LENGTH_SHORT).show()
    }

    private fun joinRoom(roomId: String) {
        socketManager.joinRoom(roomId, "test_user_123"){

        }
        appendMessage("Joined room: $roomId")
        Toast.makeText(this, "Joined room: $roomId", Toast.LENGTH_SHORT).show()
    }

    private fun sendMessage(roomId: String, message: String) {
        // Using dummy user data for testing
        socketManager.sendMessage(
            roomId = roomId,
            content = message,
            userId = "test_user_123",
            userName = "Test User",
            userImage = ""
        )
        appendMessage("Sent message: $message")
        binding.messageEditText.text.clear()
    }

    private fun sendTestBid(roomId: String) {
        // Create a sample LiveSocketModel for testing
        val sampleLiveSocket = createSampleLiveSocket(roomId)

        // Send a test bid using LiveSocketModel
        socketManager.emitBidWithLiveSocketModel(
            liveSocket = sampleLiveSocket,
            userId = "test_user_123",
            userName = "Test User",
            userImage = "",
            bidAmount = "150.00"
        )
        appendMessage("Sent test bid with LiveSocketModel: $150.00")
    }

    private fun createSampleLiveSocket(roomId: String): LiveSocketModel {
        val sampleProduct = LiveSocketModel.Product(
            id = "product_123",
            name = "Sample Product",
            price = "100.00",
            image = "https://example.com/image.jpg",
            category = "Electronics",
            status = "live",
            quantity = "1",
            isCurrent = true
        )

        val sampleSeller = LiveSocketModel.Seller(
            id = "seller_456",
            name = "Sample Seller",
            image = "https://example.com/seller.jpg",
            rating = "4.5"
        )

        val sampleHighestBid = LiveSocketModel.HighestBid(
            bidAmount = "120.00",
            userName = "Previous Bidder",
            userImage = "https://example.com/bidder.jpg",
            userId = "bidder_789",
            productId = "product_123"
        )

        return LiveSocketModel(
            products = listOf(sampleProduct),
            roomId = roomId,
            seller = sampleSeller,
            showDetail = "Sample live show for testing",
            thumbnail = "https://example.com/thumbnail.jpg",
            viewerCount = 25,
            highestBid = sampleHighestBid,
            isLive = true,
            time = System.currentTimeMillis().toString(),
            showId = "show_${System.currentTimeMillis()}",
            allowBidForAll = true,
            bidCountDown = "30",
            showTimer = "3600"
        )
    }

    private fun updateStatus(status: String) {
        binding.statusText.text = "Status: $status"
    }

    private fun appendMessage(message: String) {
        val currentText = binding.messageText.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val newMessage = "[$timestamp] $message\n"
        binding.messageText.text = currentText + newMessage
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
    }
}
