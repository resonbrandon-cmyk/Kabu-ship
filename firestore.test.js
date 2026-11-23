const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_seller_123";
const BOB_UID = "bob_student_456";
const CHARLIE_UID = "charlie_stranger_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user: denied across private orders, services, marketplace orders, and notifications", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(unauthDb.collection("services").where("available", "==", true).get());
  await assertFails(unauthDb.collection("marketplace_orders").where("buyerUid", "==", BOB_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notifications").get());
});

test("Personal SocialOrders & SellerProfile: owner isolation and terminal locking hold", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      storeName: "Kabarak Fresh Water",
      currencySymbol: "KSh ",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_101");
  const eventRef = orderRef.collection("events").doc("evt_1");
  const batch = aliceDb.batch();
  batch.set(orderRef, {
    userId: ALICE_UID,
    orderCode: "SS-1042",
    customerName: "Grace Wanjiku",
    channel: "WHATSAPP",
    handleOrPhone: "+254712345678",
    itemsSummary: "2x 20L Water Refill",
    itemCount: 2,
    totalCents: 30000,
    currencySymbol: "KSh ",
    paymentMethod: "MPESA",
    paymentStatus: "PAID",
    stage: "PAID",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(eventRef, {
    userId: ALICE_UID,
    orderId: "ord_101",
    stage: "PAID",
    title: "Order confirmed via WhatsApp",
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    orderRef.update({
      stage: "DELIVERED",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock
  await assertFails(
    orderRef.update({
      stage: "PACKING",
      updatedAt: pastDate(500),
    })
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Kabarak Services catalog: owner manages listing; buyers query available == true only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  // Alice publishes a Kabarak Water delivery service
  await assertSucceeds(
    aliceDb.collection("services").doc("svc_water_1").set({
      ownerUid: ALICE_UID,
      providerName: "Kabarak Pure Springs",
      title: "20L Clean Drinking Water Refill",
      category: "WATER",
      zone: "HOSTELS",
      description: "Sealed 20L dispenser bottle delivered straight to your hostel block or gate.",
      priceCents: 15000,
      currencySymbol: "KSh ",
      unitLabel: "per 20L bottle",
      etaLabel: "15-25 mins",
      contactChannel: "WHATSAPP",
      contactHandle: "+254711000111",
      available: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob queries available services (succeeds)
  await assertSucceeds(
    bobDb.collection("services").where("available", "==", true).get()
  );

  // Unfiltered query on services must fail
  await assertFails(bobDb.collection("services").get());

  // Bob cannot modify or delete Alice's service
  await assertFails(
    bobDb.collection("services").doc("svc_water_1").update({
      priceCents: 100,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bobDb.collection("services").doc("svc_water_1").delete());
});

test("Kabarak Marketplace Order: atomic create + seller notification + interactive chat + stage transitions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(20000);

  // 1. Alice lists a food service near Main Gate
  await assertSucceeds(
    aliceDb.collection("services").doc("svc_food_1").set({
      ownerUid: ALICE_UID,
      providerName: "Mama Faith Kitchen",
      title: "Beef Pilau & Kachumbari Bowl",
      category: "FOOD",
      zone: "MAIN_GATE",
      description: "Hot spiced beef pilau packed fresh for students and community.",
      priceCents: 25000,
      currencySymbol: "KSh ",
      unitLabel: "per plate",
      etaLabel: "20 mins",
      contactChannel: "WHATSAPP",
      contactHandle: "+254722334455",
      available: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 2. Bob places order + event + seller notification atomically in one WriteBatch
  const orderRef = bobDb.collection("marketplace_orders").doc("mk_ord_1");
  const eventRef = orderRef.collection("events").doc("evt_init");
  const msgRef = orderRef.collection("messages").doc("msg_init");
  const sellerNotifRef = bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1");

  const orderBatch = bobDb.batch();
  orderBatch.set(orderRef, {
    orderCode: "KB-2041",
    serviceId: "svc_food_1",
    serviceTitle: "Beef Pilau & Kachumbari Bowl",
    category: "FOOD",
    buyerUid: BOB_UID,
    buyerName: "Bob Kiprop",
    buyerHandleOrPhone: "+254700112233",
    sellerUid: ALICE_UID,
    sellerName: "Mama Faith Kitchen",
    sellerHandleOrPhone: "+254722334455",
    deliveryZone: "HOSTELS",
    deliveryDetails: "Men's Hostel Block C, Room 12",
    quantity: 2,
    unitPriceCents: 25000,
    totalCents: 50000,
    currencySymbol: "KSh ",
    paymentMethod: "MPESA",
    paymentStatus: "PENDING",
    stage: "NEW_DM",
    notes: "Extra kachumbari please",
    lastMessagePreview: "Extra kachumbari please",
    unreadForSeller: true,
    unreadForBuyer: false,
    createdAt: ts,
    updatedAt: ts,
  });
  orderBatch.set(eventRef, {
    orderId: "mk_ord_1",
    buyerUid: BOB_UID,
    sellerUid: ALICE_UID,
    actorUid: BOB_UID,
    stage: "NEW_DM",
    title: "Order placed by Bob Kiprop",
    note: "Men's Hostel Block C, Room 12",
    createdAt: ts,
  });
  orderBatch.set(msgRef, {
    orderId: "mk_ord_1",
    buyerUid: BOB_UID,
    sellerUid: ALICE_UID,
    senderUid: BOB_UID,
    senderName: "Bob Kiprop",
    senderRole: "BUYER",
    text: "Extra kachumbari please",
    createdAt: ts,
  });
  orderBatch.set(sellerNotifRef, {
    recipientUid: ALICE_UID,
    senderUid: BOB_UID,
    orderId: "mk_ord_1",
    orderCode: "KB-2041",
    title: "New Order: Beef Pilau & Kachumbari Bowl",
    body: "Bob Kiprop ordered 2x · Student Hostels",
    type: "NEW_ORDER",
    read: false,
    createdAt: ts,
  });
  await assertSucceeds(orderBatch.commit());

  // 3. Alice (seller) queries her incoming orders & unread notifications
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").where("sellerUid", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get()
  );
  // Seller queries events & messages scoped by sellerUid
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc("mk_ord_1").collection("events").where("sellerUid", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc("mk_ord_1").collection("messages").where("sellerUid", "==", ALICE_UID).get()
  );

  // 4. Alice marks notification read, replies in chat, and advances stage to OUT_FOR_DELIVERY
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      read: true,
    })
  );

  const replyBatch = aliceDb.batch();
  replyBatch.set(
    aliceDb.collection("marketplace_orders").doc("mk_ord_1").collection("messages").doc("msg_2"),
    {
      orderId: "mk_ord_1",
      buyerUid: BOB_UID,
      sellerUid: ALICE_UID,
      senderUid: ALICE_UID,
      senderName: "Mama Faith Kitchen",
      senderRole: "SELLER",
      text: "Packing now! Rider Samuel is leaving Main Gate in 5 mins.",
      createdAt: pastDate(4000),
    }
  );
  replyBatch.update(aliceDb.collection("marketplace_orders").doc("mk_ord_1"), {
    lastMessagePreview: "Packing now! Rider Samuel is leaving Main Gate in 5 mins.",
    unreadForBuyer: true,
    unreadForSeller: false,
    updatedAt: pastDate(4000),
  });
  replyBatch.set(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_buyer_1"),
    {
      recipientUid: BOB_UID,
      senderUid: ALICE_UID,
      orderId: "mk_ord_1",
      orderCode: "KB-2041",
      title: "Message from Mama Faith Kitchen",
      body: "Packing now! Rider Samuel is leaving Main Gate in 5 mins.",
      type: "NEW_MESSAGE",
      read: false,
      createdAt: pastDate(4000),
    }
  );
  await assertSucceeds(replyBatch.commit());

  // 5. Charlie (stranger) cannot read, list, message, or spoof notifications
  await assertFails(charlieDb.collection("marketplace_orders").doc("mk_ord_1").get());
  await assertFails(
    charlieDb.collection("marketplace_orders").doc("mk_ord_1").collection("messages").doc("msg_bad").set({
      orderId: "mk_ord_1",
      buyerUid: BOB_UID,
      sellerUid: ALICE_UID,
      senderUid: CHARLIE_UID,
      senderName: "Stranger",
      senderRole: "BUYER",
      text: "Hello",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(
    charlieDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_spam").set({
      recipientUid: ALICE_UID,
      senderUid: CHARLIE_UID,
      orderId: "mk_ord_1",
      orderCode: "KB-2041",
      title: "Spam",
      body: "Fake alert",
      type: "NEW_ORDER",
      read: false,
      createdAt: pastDate(1000),
    })
  );

  // 6. Seller delivers order -> terminal lock prevents buyer/seller stage changes afterward
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc("mk_ord_1").update({
      stage: "DELIVERED",
      paymentStatus: "PAID",
      unreadForBuyer: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("marketplace_orders").doc("mk_ord_1").update({
      stage: "PACKING",
      updatedAt: pastDate(500),
    })
  );
});
