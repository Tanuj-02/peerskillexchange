I'll help you create an API endpoints file showcasing the usage of your endpoints. Let me first explore the controllers in your project to understand what endpoints are available.

Now let me read the controller files to understand the endpoints:

Now let me check the DTOs to understand request/response structures:

Perfect! Now I have all the information I need. Let me create a comprehensive API endpoints documentation file. I'll create a markdown file that showcases all the API endpoints with usage examples.

Now I'll create a comprehensive API endpoints documentation file:

I'll create the API documentation file now. Based on my analysis of the controllers and DTOs, here's a comprehensive API endpoints file:

## **API_ENDPOINTS.md**

```markdown
# Peer Skill Exchange - API Endpoints Documentation

## Base URL
```
http://localhost:8080
```

## Authentication
Most endpoints require JWT Bearer token authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. AUTH ENDPOINTS (`/api/auth`)

### 1.1 User Registration
**POST** `/api/auth/register`

Register a new user in the system.

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePassword123",
  "name": "John Doe",
  "bio": "I love teaching and learning new skills",
  "profilePicture": "https://example.com/image.jpg",
  "college": "XYZ University"
}
```

**Response (201 Created):**
```json
{
  "id": "user-123",
  "username": "johndoe",
  "email": "john@example.com",
  "name": "John Doe",
  "bio": "I love teaching and learning new skills",
  "profilePicture": "https://example.com/image.jpg",
  "college": "XYZ University",
  "role": "USER"
}
```

---

### 1.2 User Login
**POST** `/api/auth/login`

Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "usernameOrEmail": "johndoe",
  "password": "SecurePassword123"
}
```

**Response (200 OK):**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.3 Verify Email
**GET** `/api/auth/verify-email`

Verify user email with verification code sent during registration.

**Query Parameters:**
- `email` (required): User's email address
- `code` (required): Verification code sent to email

**Example:**
```
GET /api/auth/verify-email?email=john@example.com&code=123456
```

**Response (200 OK):**
```json
"Email verified successfully"
```

---

### 1.4 Resend Verification Email
**POST** `/api/auth/resend-verification`

Resend verification code to user's email.

**Query Parameters:**
- `email` (required): User's email address

**Example:**
```
POST /api/auth/resend-verification?email=john@example.com
```

**Response (200 OK):**
```json
"Verification email sent successfully"
```

---

## 2. USER ENDPOINTS (`/api/users`)

### 2.1 Get Current User Profile
**GET** `/api/users/me`

Get the profile of the currently authenticated user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "id": "user-123",
  "name": "John Doe",
  "username": "johndoe",
  "bio": "I love teaching and learning new skills",
  "college": "XYZ University",
  "profilePicture": "https://example.com/image.jpg",
  "email": "john@example.com",
  "role": "USER",
  "skills": {
    "canTeach": ["JavaScript", "Python", "Web Development"],
    "wantsToLearn": ["Machine Learning", "Data Science"]
  }
}
```

---

### 2.2 Get User by ID
**GET** `/api/users/{id}`

Get profile information of a specific user.

**Path Parameters:**
- `id` (required): User ID

**Example:**
```
GET /api/users/user-456
```

**Response (200 OK):**
```json
{
  "id": "user-456",
  "name": "Jane Smith",
  "username": "janesmith",
  "bio": "Passionate about web design",
  "college": "ABC University",
  "profilePicture": "https://example.com/jane.jpg",
  "email": "jane@example.com",
  "role": "USER",
  "skills": {
    "canTeach": ["UI/UX Design", "CSS", "Figma"],
    "wantsToLearn": ["Backend Development"]
  }
}
```

---

### 2.3 Update User Profile
**PUT** `/api/users/me/update`

Update the current user's profile information.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "id": "user-123",
  "name": "John Doe Updated",
  "username": "johndoe",
  "email": "newemail@example.com",
  "profilePicture": "https://example.com/newimage.jpg",
  "bio": "Updated bio",
  "college": "New University"
}
```

**Response (200 OK):**
```json
{
  "id": "user-123",
  "name": "John Doe Updated",
  "username": "johndoe",
  "bio": "Updated bio",
  "college": "New University",
  "profilePicture": "https://example.com/newimage.jpg",
  "email": "newemail@example.com",
  "role": "USER",
  "skills": {
    "canTeach": ["JavaScript", "Python"],
    "wantsToLearn": ["Machine Learning"]
  }
}
```

---

### 2.4 Send Password Change Verification Code
**POST** `/api/users/me/password/send-verification`

Send a verification code to the user's email before changing password.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
"Verification code sent to your email"
```

---

### 2.5 Update Password
**PUT** `/api/users/me/password`

Update user password with verification code.

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `oldPassword` (required): Current password
- `newPassword` (required): New password
- `verificationCode` (required): Code received via email

**Example:**
```
PUT /api/users/me/password?oldPassword=OldPass123&newPassword=NewPass456&verificationCode=654321
```

**Response (200 OK):**
```json
"Password updated successfully"
```

---

### 2.6 Search Users
**GET** `/api/users/search`

Search for users with optional filters (pagination supported).

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `q` (optional, default=""): Search query (name or username)
- `skill` (optional): Filter by skill
- `type` (optional, default="BOTH"): Filter by skill type - "TEACH", "LEARN", or "BOTH"
- `page` (optional, default=0): Page number (0-indexed)
- `size` (optional, default=10): Page size (max 50)

**Examples:**
```
GET /api/users/search?q=john
GET /api/users/search?skill=JavaScript&type=TEACH&page=0&size=20
GET /api/users/search?q=doe&skill=Python&type=BOTH
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "user-123",
      "name": "John Doe",
      "username": "johndoe",
      "bio": "I love teaching and learning",
      "college": "XYZ University",
      "profilePicture": "https://example.com/image.jpg",
      "email": "john@example.com",
      "role": "USER",
      "skills": {
        "canTeach": ["JavaScript", "Python"],
        "wantsToLearn": ["Machine Learning"]
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

## 3. SKILL ENDPOINTS (`/api/skills`)

### 3.1 Add/Update User Skills
**POST** `/api/skills/add`

Add or update skills that user can teach and wants to learn.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "canTeach": ["JavaScript", "Python", "Web Development"],
  "wantsToLearn": ["Machine Learning", "Data Science", "Cloud Computing"]
}
```

**Response (200 OK):**
```json
"Skills added successfully"
```

---

## 4. EXCHANGE REQUEST ENDPOINTS (`/api/exchange-requests`)

### 4.1 Create Exchange Request
**POST** `/api/exchange-requests/create`

Send a skill exchange request to another user.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "receiverId": "user-456",
  "message": "I want to learn Web Design from you",
  "canTeach": ["JavaScript", "Python"],
  "wantToLearn": ["UI/UX Design"]
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "sender": {
    "id": "user-123",
    "name": "John Doe",
    "username": "johndoe",
    "bio": "Teacher",
    "college": "XYZ University",
    "profilePicture": "https://example.com/john.jpg",
    "email": "john@example.com",
    "role": "USER",
    "skills": {
      "canTeach": ["JavaScript", "Python"],
      "wantsToLearn": ["UI/UX Design"]
    }
  },
  "receiver": {
    "id": "user-456",
    "name": "Jane Smith",
    "username": "janesmith",
    "bio": "Designer",
    "college": "ABC University",
    "profilePicture": "https://example.com/jane.jpg",
    "email": "jane@example.com",
    "role": "USER",
    "skills": {
      "canTeach": ["UI/UX Design"],
      "wantsToLearn": ["JavaScript"]
    }
  },
  "message": "I want to learn Web Design from you",
  "status": "PENDING",
  "canTeach": ["JavaScript", "Python"],
  "wantToLearn": ["UI/UX Design"],
  "createdAt": "2026-05-09T18:06:48"
}
```

---

### 4.2 Get My Sent Requests
**GET** `/api/exchange-requests/me/sent`

Get all exchange requests sent by the current user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "sender": { /* sender details */ },
    "receiver": { /* receiver details */ },
    "message": "I want to learn Web Design",
    "status": "PENDING",
    "canTeach": ["JavaScript"],
    "wantToLearn": ["UI/UX Design"],
    "createdAt": "2026-05-09T18:06:48"
  }
]
```

---

### 4.3 Get My Received Requests
**GET** `/api/exchange-requests/me/received`

Get all exchange requests received by the current user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": 2,
    "sender": { /* sender details */ },
    "receiver": { /* receiver details */ },
    "message": "Let's exchange skills",
    "status": "PENDING",
    "canTeach": ["UI/UX Design"],
    "wantToLearn": ["Python"],
    "createdAt": "2026-05-09T17:35:07"
  }
]
```

---

### 4.4 Accept Exchange Request
**PUT** `/api/exchange-requests/{id}/accept`

Accept an exchange request.

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `id` (required): Exchange request ID

**Example:**
```
PUT /api/exchange-requests/2/accept
```

**Response (200 OK):**
```json
{
  "id": 2,
  "sender": { /* sender details */ },
  "receiver": { /* receiver details */ },
  "message": "Let's exchange skills",
  "status": "ACCEPTED",
  "canTeach": ["UI/UX Design"],
  "wantToLearn": ["Python"],
  "createdAt": "2026-05-09T17:35:07"
}
```

---

### 4.5 Reject Exchange Request
**PUT** `/api/exchange-requests/{id}/reject`

Reject an exchange request.

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `id` (required): Exchange request ID

**Example:**
```
PUT /api/exchange-requests/2/reject
```

**Response (200 OK):**
```json
{
  "id": 2,
  "sender": { /* sender details */ },
  "receiver": { /* receiver details */ },
  "message": "Let's exchange skills",
  "status": "REJECTED",
  "canTeach": ["UI/UX Design"],
  "wantToLearn": ["Python"],
  "createdAt": "2026-05-09T17:35:07"
}
```

---

### 4.6 Disconnect/Remove Exchange
**DELETE** `/api/exchange-requests/disconnect/{otherUserId}`

Disconnect from an existing exchange with another user.

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `otherUserId` (required): ID of the user to disconnect from

**Example:**
```
DELETE /api/exchange-requests/disconnect/user-456
```

**Response (200 OK):**
```json
"Disconnected successfully"
```

---

## 5. CHAT ENDPOINTS (`/api/chats`)

### 5.1 Get My Conversations
**GET** `/api/chats`

Get all conversations of the current user (paginated).

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `page` (optional, default=0): Page number (0-indexed)
- `size` (optional, default=20): Page size (max 50)

**Examples:**
```
GET /api/chats
GET /api/chats?page=0&size=20
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "otherUserId": "user-456",
      "otherUsername": "janesmith",
      "otherName": "Jane Smith",
      "otherProfilePicture": "https://example.com/jane.jpg",
      "createdAt": "2026-05-09T18:00:22"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

### 5.2 Get Conversation Messages
**GET** `/api/chats/{conversationId}/messages`

Get all messages in a specific conversation (paginated).

**Headers:**
```
Authorization: Bearer <token>
```

**Path Parameters:**
- `conversationId` (required): Conversation ID

**Query Parameters:**
- `page` (optional, default=0): Page number (0-indexed)
- `size` (optional, default=50): Page size (max 100)

**Examples:**
```
GET /api/chats/1/messages
GET /api/chats/1/messages?page=0&size=50
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "conversationId": 1,
      "senderId": "user-123",
      "senderUsername": "johndoe",
      "receiverId": "user-456",
      "receiverUsername": "janesmith",
      "content": "Hi Jane! How are you?",
      "createdAt": "2026-05-09T18:06:48"
    },
    {
      "id": 2,
      "conversationId": 1,
      "senderId": "user-456",
      "senderUsername": "janesmith",
      "receiverId": "user-123",
      "receiverUsername": "johndoe",
      "content": "I'm good! Ready to learn JavaScript?",
      "createdAt": "2026-05-09T18:06:55"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50,
    "totalPages": 1,
    "totalElements": 2
  }
}
```

---

## 6. DISCOVER ENDPOINTS (`/api/discover`)

### 6.1 Discover Users
**GET** `/api/discover`

Discover other users based on skill compatibility and recommendations (paginated).

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `page` (optional, default=0): Page number (0-indexed)
- `size` (optional, default=10): Page size (max 50)

**Examples:**
```
GET /api/discover
GET /api/discover?page=0&size=15
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "user-789",
      "name": "Alex Kumar",
      "username": "alexkumar",
      "bio": "Enthusiastic learner and teacher",
      "college": "DEF University",
      "profilePicture": "https://example.com/alex.jpg",
      "email": "alex@example.com",
      "role": "USER",
      "skills": {
        "canTeach": ["Data Science", "Python"],
        "wantsToLearn": ["Web Development"]
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalPages": 1,
    "totalElements": 1
  }
}
```

---

## WebSocket Endpoints

### Real-time Chat
**WebSocket** `/ws`

WebSocket endpoint for real-time messaging. Messages sent through WebSocket are automatically persisted.

**Connection:**
```
ws://localhost:8080/ws
```

---

## Error Responses

### 400 Bad Request
```json
{
  "status": 400,
  "message": "Invalid request parameters",
  "timestamp": "2026-05-09T18:06:48"
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Unauthorized - Invalid or missing token",
  "timestamp": "2026-05-09T18:06:48"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "message": "Forbidden - Access denied",
  "timestamp": "2026-05-09T18:06:48"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "message": "Resource not found",
  "timestamp": "2026-05-09T18:06:48"
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Internal server error",
  "timestamp": "2026-05-09T18:06:48"
}
```

---

## Usage Examples

### Complete Flow Example

#### 1. Register a New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123",
    "name": "John Doe",
    "bio": "Learning and teaching skills",
    "profilePicture": "https://example.com/john.jpg",
    "college": "XYZ University"
  }'
```

#### 2. Verify Email
```bash
curl -X GET "http://localhost:8080/api/auth/verify-email?email=john@example.com&code=123456"
```

#### 3. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "SecurePass123"
  }'
```

#### 4. Add Skills
```bash
curl -X POST http://localhost:8080/api/skills/add \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "canTeach": ["JavaScript", "Python"],
    "wantsToLearn": ["Machine Learning"]
  }'
```

#### 5. Search Users
```bash
curl -X GET "http://localhost:8080/api/users/search?skill=Python&type=TEACH" \
  -H "Authorization: Bearer <your_token>"
```

#### 6. Create Exchange Request
```bash
curl -X POST http://localhost:8080/api/exchange-requests/create \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": "user-456",
    "message": "I want to learn Machine Learning from you",
    "canTeach": ["JavaScript"],
    "wantToLearn": ["Machine Learning"]
  }'
```

---

## Rate Limiting
Currently, there are no explicit rate limits on the API. However, it's recommended to implement rate limiting in production for security purposes.

## Support
For issues or questions regarding the API, please contact the development team.

---

**Last Updated:** May 9, 2026
```

## Summary

I've created a comprehensive **API_ENDPOINTS.md** file that documents all endpoints in your Peer Skill Exchange application. The documentation includes:

✅ **6 Main API Categories:**
1. **Auth Endpoints** - Registration, login, email verification
2. **User Endpoints** - Profile management, search, password updates
3. **Skill Endpoints** - Adding/updating user skills
4. **Exchange Request Endpoints** - Creating and managing skill exchanges
5. **Chat Endpoints** - Conversations and messaging
6. **Discover Endpoints** - Finding compatible users

✅ **For Each Endpoint:**
- HTTP method and URL
- Description
- Required headers/authentication
- Request body examples (JSON)
- Response examples (JSON)
- Query parameters and path parameters
- Error responses

✅ **Additional Sections:**
- Authentication guide
- WebSocket information
- Complete workflow examples with curl commands
- Error handling patterns

You can copy this content and create an **API_ENDPOINTS.md** file in your project root or documentation folder. This will serve as excellent documentation for developers integrating with your API!