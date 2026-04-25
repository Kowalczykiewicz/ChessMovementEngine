# Chess Movement Engine (Scala + ZIO)

A functional backend system for simulating and manipulating chess pieces on a virtual board via a REST API.
![chess.png](doc%2Fchess.png)

## Domain Overview

The system models a simplified chess environment with the following rules:

- The board is always **8×8**, represented as integer coordinates `(x, y)`.
- Each field can contain at most **one piece**.
- All pieces are of the same color (no capturing or interactions).
- Supported pieces:
    - **Rook** – moves horizontally or vertically across any number of empty fields.
    - **Bishop** – moves diagonally across any number of empty fields.
- Each piece has a **unique identifier** assigned on creation.
- Pieces can be placed and moved across valid fields using their ID.

### Optional Rule
- Pieces can be removed from the board, while preserving their last position.
- Removed pieces cannot be re-added to the board.

---

## System Capabilities
The application provides functionality to:

- Place a new chess piece on the board
- Move an existing piece according to its movement rules
- Remove a piece while retaining its historical position