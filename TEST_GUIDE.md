# MNCO Lab Orchestrator - Test Guide (Multi-Tenant & Isolated Instances)

This guide outlines the steps to verify the newly implemented multi-tenant architecture and isolated lab instances.

## 1. Setup & Pre-requisites
- Ensure EVE-NG server is reachable.
- Backend is running (`mvn spring-boot:run` in `mnco-backend`).
- Frontend is running (`npm run dev` in `mnco-frontend`).

## 2. Test Scenarios

### A. Admin Setup & Template Sync
1. **Login as ADMIN** (Default: `admin` / `admin123` or your configured admin).
2. Navigate to **Lab Templates** via Sidebar.
3. Click **Sync from EVE-NG**.
4. **Verify**: All templates from EVE-NG (e.g., in `/opt/unetlab/labs/templates/`) appear in the grid.

### B. User Creation & EVE-NG Sync
1. **Register a new user** via the UI (e.g., username: `student1`, role: `STUDENT`).
2. **Login** as `student1`.
3. **Verify in EVE-NG**: Check the EVE-NG User Management. A user `student1` should now exist.
   - *Note*: If you don't have access to EVE-NG UI, the backend logs will show `Creating user 'student1' in EVE-NG`.

### C. Lab Assignment (Teacher/Admin Flow)
1. **Login as ADMIN or TEACHER**.
2. Navigate to **Lab Templates**.
3. Find a template and click **Assign to User**.
4. Select `student1` from the modal and confirm.
5. **Verify**: Success message appears.

### D. Student Lifecycle (The "Wow" Moment)
1. **Login as `student1`**.
2. Go to **My Labs** (Dashboard).
3. **Verify**: The assigned lab appears. Status is **STOPPED**.
4. Click **Start**.
   - *Backend Action*: Clones the template to `/opt/unetlab/labs/instances/student1/...unl` and starts nodes.
5. Wait for status to turn **RUNNING**.
6. Click **View Nodes**.
7. **Verify**: A list of nodes appears with **Console** buttons.
8. Click **Console** on a node.
   - *Verify*: Opens in a **new tab** pointing to the HTML5 console of YOUR private instance.
9. Click **Reset** (while running or stopped).
   - *Verify*: The instance is deleted in EVE-NG and re-cloned from the master template.

### E. Multi-Tenant Parallelism (Crucial Test)
1. **Register a second user** `student2`.
2. **Login as ADMIN** and assign the **same template** to `student2`.
3. **Login as `student2`** in a private browser window.
4. **Start** the lab as `student2`.
5. **Verify**: Both `student1` and `student2` can have their labs **RUNNING simultaneously**.
   - *Technical Check*: In EVE-NG, you should see two different `.unl` files in their respective folders under `/opt/unetlab/labs/instances/`.

## 3. Verification of "html5: 1" Requirement
- When you open a console, the URL should look like: `http://{eveng-ip}/html5/#/client/...`
- If it opens a `telnet://` link, the EVE-NG user profile was not correctly synced with `html5: 1`.

## 4. Reset Logic
- Change a configuration inside a router/switch as `student1`.
- Close the console.
- Click **Reset** in the MNCO Dashboard.
- **Start** again and check the console.
- **Verify**: The changes are gone. The lab is back to its master template state.
