# PayPal Webhook Server

This server receives PayPal webhook notifications and stores payment data in a JSON file.

## Setup Instructions

### 1. Deploy to Railway (Free)

1. Create account at [railway.app](https://railway.app)
2. Connect your GitHub repository
3. Deploy this `webhook-server` folder
4. Railway will automatically use the Dockerfile

### 2. Configure PayPal Webhooks

1. Go to [PayPal Developer Dashboard](https://developer.paypal.com/developer/applications/)
2. Select your app
3. Go to "Webhooks" section
4. Add webhook URL: `https://your-railway-app.railway.app/paypal/webhook`
5. Select events to listen for:
   - `PAYMENT.SALE.COMPLETED`
   - `PAYMENTS.PAYMENT.CREATED`
   - `CHECKOUT.ORDER.COMPLETED`

### 3. Test the Setup

1. Visit `https://your-railway-app.railway.app/status` to check if server is running
2. Make a test payment through PayPal
3. Check server logs to see if webhook was received
4. The `payments.json` file will be created automatically

### 4. Connect to Minecraft Server

1. Download the `payments.json` file from your webhook server
2. Place it in your Minecraft server directory (same folder as the mod)
3. Run `/fund` command to see payment data

## Local Testing

```bash
# Compile and run locally
javac -cp gson-2.10.1.jar PayPalWebhookServer.java
java -cp .:gson-2.10.1.jar com.example.webhook.PayPalWebhookServer
```

Use ngrok for local webhook testing:
```bash
ngrok http 8080
```

Then use the ngrok URL as your PayPal webhook endpoint.

## Files Created

- `payments.json` - Stores all payment webhook data
- Server logs - Shows webhook activity

## Troubleshooting

- Check Railway deployment logs for errors
- Verify webhook URL is accessible (test `/status` endpoint)
- Ensure PayPal webhook events are properly configured
- Check that webhook signatures are valid (if implemented)