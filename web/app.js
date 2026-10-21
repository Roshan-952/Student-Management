/**
 * EduTrack Pro — Student Management System Client Application
 * Vanilla JavaScript (Zero External Dependencies)
 */

document.addEventListener('DOMContentLoaded', () => {
    // Application State
    const state = {
        students: [],
        filteredStudents: [],
        stats: null,
        dbStatus: null,
        searchQuery: '',
        activeGradeFilter: 'ALL',
        currentSort: 'id-asc',
        editingStudentId: null,
        pendingDeleteId: null,
        theme: localStorage.getItem('edutrack_theme') || 'dark'
    };

    // DOM Elements Cache
    const elements = {
        html: document.documentElement,
        themeToggle: document.getElementById('theme-toggle'),
        themeIconSun: document.getElementById('theme-icon-sun'),
        themeIconMoon: document.getElementById('theme-icon-moon'),
        
        // DB Elements
        dbStatusBtn: document.getElementById('btn-open-db-settings'),
        dbStatusDot: document.getElementById('db-status-dot'),
        dbStatusText: document.getElementById('db-status-text'),
        footerDbLink: document.getElementById('footer-db-link'),
        footerDbName: document.getElementById('footer-db-name'),

        // Stats Elements
        statTotal: document.getElementById('stat-total-students'),
        statAvgMarks: document.getElementById('stat-average-marks'),
        statAvgGrade: document.getElementById('stat-average-grade'),
        statTopStudent: document.getElementById('stat-top-student'),
        statHighestMarks: document.getElementById('stat-highest-marks'),
        gradeDistContainer: document.getElementById('grade-distribution-container'),

        // Toolbar Elements
        inputSearch: document.getElementById('input-search'),
        btnClearSearch: document.getElementById('btn-clear-search'),
        filterPills: document.getElementById('filter-pills'),
        selectSort: document.getElementById('select-sort'),
        btnExportCsv: document.getElementById('btn-export-csv'),
        btnOpenAddModal: document.getElementById('btn-open-add-modal'),
        btnEmptyAdd: document.getElementById('btn-empty-add'),

        // Table Elements
        tableBody: document.getElementById('students-tbody'),
        emptyState: document.getElementById('empty-state'),

        // Student Modal Elements
        modalStudent: document.getElementById('modal-student'),
        modalStudentTitle: document.getElementById('modal-student-title'),
        formStudent: document.getElementById('form-student'),
        btnCloseStudentModal: document.getElementById('btn-close-student-modal'),
        btnCancelStudentModal: document.getElementById('btn-cancel-student-modal'),
        inputStudentId: document.getElementById('student-edit-id'),
        inputStudentName: document.getElementById('input-student-name'),
        inputStudentEmail: document.getElementById('input-student-email'),
        inputStudentCourse: document.getElementById('input-student-course'),
        sliderStudentMarks: document.getElementById('slider-student-marks'),
        inputStudentMarks: document.getElementById('input-student-marks'),
        previewGradeBadge: document.getElementById('preview-grade-badge'),
        btnSubmitText: document.getElementById('btn-submit-text'),

        // Delete Modal Elements
        modalDelete: document.getElementById('modal-delete'),
        deleteStudentName: document.getElementById('delete-student-name'),
        deleteStudentId: document.getElementById('delete-student-id'),
        btnCancelDelete: document.getElementById('btn-cancel-delete'),
        btnConfirmDelete: document.getElementById('btn-confirm-delete'),

        // Database Modal Elements
        modalDbSettings: document.getElementById('modal-db-settings'),
        btnCloseDbModal: document.getElementById('btn-close-db-modal'),
        tabDbEmbedded: document.getElementById('tab-db-embedded'),
        tabDbMysql: document.getElementById('tab-db-mysql'),
        tabContentEmbedded: document.getElementById('tab-content-embedded'),
        tabContentMysql: document.getElementById('tab-content-mysql'),
        btnApplyEmbedded: document.getElementById('btn-apply-embedded'),
        formMysqlConfig: document.getElementById('form-mysql-config'),
        inputMysqlHost: document.getElementById('input-mysql-host'),
        inputMysqlPort: document.getElementById('input-mysql-port'),
        inputMysqlDb: document.getElementById('input-mysql-db'),
        inputMysqlUser: document.getElementById('input-mysql-user'),
        inputMysqlPass: document.getElementById('input-mysql-pass'),

        // Toast Container
        toastContainer: document.getElementById('toast-container')
    };

    // ==========================================
    // Initialization
    // ==========================================

    function init() {
        applyTheme(state.theme);
        bindEvents();
        refreshAll();
    }

    function refreshAll() {
        fetchDbStatus();
        fetchStudents();
        fetchStats();
    }

    // ==========================================
    // Theme Switcher
    // ==========================================

    function applyTheme(theme) {
        state.theme = theme;
        elements.html.setAttribute('data-theme', theme);
        localStorage.setItem('edutrack_theme', theme);

        if (theme === 'light') {
            elements.themeIconSun.classList.add('hidden');
            elements.themeIconMoon.classList.remove('hidden');
        } else {
            elements.themeIconSun.classList.remove('hidden');
            elements.themeIconMoon.classList.add('hidden');
        }
    }

    function toggleTheme() {
        applyTheme(state.theme === 'dark' ? 'light' : 'dark');
    }

    // ==========================================
    // Cloud Demo Mode & Offline Storage Fallback
    // ==========================================

    const INITIAL_DEMO_STUDENTS = [
        { id: 101, name: 'Aarav Sharma', email: 'aarav.sharma@example.edu', course: 'Computer Science', marks: 94.5, grade: 'A+' },
        { id: 102, name: 'Ananya Iyer', email: 'ananya.iyer@example.edu', course: 'Information Technology', marks: 88.0, grade: 'A' },
        { id: 103, name: 'Rohan Verma', email: 'rohan.v@example.edu', course: 'Data Science', marks: 76.5, grade: 'B' },
        { id: 104, name: 'Priya Patel', email: 'priya.patel@example.edu', course: 'Software Engineering', marks: 91.0, grade: 'A+' },
        { id: 105, name: 'Vikram Malhotra', email: 'vikram.m@example.edu', course: 'Artificial Intelligence', marks: 68.0, grade: 'C' }
    ];

    function calculateGrade(marks) {
        if (marks >= 90) return 'A+';
        if (marks >= 80) return 'A';
        if (marks >= 70) return 'B';
        if (marks >= 60) return 'C';
        if (marks >= 50) return 'D';
        return 'F';
    }

    function getLocalStudents() {
        const stored = localStorage.getItem('edutrack_students_demo');
        if (stored) {
            try { return JSON.parse(stored); } catch (e) {}
        }
        localStorage.setItem('edutrack_students_demo', JSON.stringify(INITIAL_DEMO_STUDENTS));
        return [...INITIAL_DEMO_STUDENTS];
    }

    function saveLocalStudents(list) {
        localStorage.setItem('edutrack_students_demo', JSON.stringify(list));
    }

    function calculateLocalStats(students) {
        const total = students.length;
        if (total === 0) {
            return {
                total: 0,
                averageMarks: 0,
                averageGrade: 'N/A',
                topStudent: 'None',
                highestMarks: 0,
                gradeDistribution: { 'A+': 0, 'A': 0, 'B': 0, 'C': 0, 'D': 0, 'F': 0 }
            };
        }

        const sumMarks = students.reduce((acc, s) => acc + s.marks, 0);
        const avg = sumMarks / total;
        let top = students[0];
        const dist = { 'A+': 0, 'A': 0, 'B': 0, 'C': 0, 'D': 0, 'F': 0 };

        students.forEach(s => {
            if (s.marks > top.marks) top = s;
            const g = s.grade || calculateGrade(s.marks);
            dist[g] = (dist[g] || 0) + 1;
        });

        return {
            total,
            averageMarks: avg,
            averageGrade: calculateGrade(avg),
            topStudent: top.name,
            highestMarks: top.marks,
            gradeDistribution: dist
        };
    }

    // ==========================================
    // API Requests (with transparent Cloud Demo fallback)
    // ==========================================

    async function fetchDbStatus() {
        try {
            const res = await fetch('/api/db-status');
            if (!res.ok) throw new Error('Status network error');
            const data = await res.json();
            state.isDemoMode = false;
            state.dbStatus = data;
            renderDbStatus(data);
        } catch (err) {
            state.isDemoMode = true;
            elements.dbStatusDot.className = 'status-pulse';
            elements.dbStatusDot.style.background = '#38bdf8';
            elements.dbStatusText.textContent = 'Cloud Demo (Local Storage)';
            elements.footerDbName.textContent = 'Local Browser Storage (Cloud Demo)';
        }
    }

    async function fetchStudents() {
        if (state.isDemoMode) {
            state.students = getLocalStudents();
            applyFilterAndSort();
            return;
        }
        try {
            const res = await fetch('/api/students');
            if (!res.ok) throw new Error('Could not fetch students');
            state.students = await res.json();
            applyFilterAndSort();
        } catch (err) {
            state.isDemoMode = true;
            state.students = getLocalStudents();
            applyFilterAndSort();
            elements.dbStatusDot.className = 'status-pulse';
            elements.dbStatusDot.style.background = '#38bdf8';
            elements.dbStatusText.textContent = 'Cloud Demo (Local Storage)';
            elements.footerDbName.textContent = 'Local Browser Storage (Cloud Demo)';
        }
    }

    async function fetchStats() {
        if (state.isDemoMode) {
            state.stats = calculateLocalStats(state.students);
            renderStats(state.stats);
            return;
        }
        try {
            const res = await fetch('/api/stats');
            if (!res.ok) throw new Error('Stats error');
            state.stats = await res.json();
            renderStats(state.stats);
        } catch (err) {
            state.stats = calculateLocalStats(state.students);
            renderStats(state.stats);
        }
    }

    async function saveStudent(studentData) {
        studentData.grade = calculateGrade(studentData.marks);

        if (state.isDemoMode) {
            const isEdit = Boolean(state.editingStudentId);
            let list = getLocalStudents();
            if (isEdit) {
                const idx = list.findIndex(s => s.id === state.editingStudentId);
                if (idx !== -1) {
                    list[idx] = { ...list[idx], ...studentData, id: state.editingStudentId };
                }
            } else {
                const nextId = list.length > 0 ? Math.max(...list.map(s => s.id)) + 1 : 101;
                studentData.id = nextId;
                list.push(studentData);
            }
            saveLocalStudents(list);
            state.students = list;
            return studentData;
        }

        const isEdit = Boolean(state.editingStudentId);
        const url = isEdit ? `/api/students/${state.editingStudentId}` : '/api/students';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(studentData)
        });

        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Failed to save student');
        }
        return data;
    }

    async function deleteStudent(id) {
        if (state.isDemoMode) {
            let list = getLocalStudents();
            list = list.filter(s => s.id !== id);
            saveLocalStudents(list);
            state.students = list;
            return { message: 'Deleted from local storage' };
        }

        const res = await fetch(`/api/students/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Failed to delete record');
        }
        return data;
    }

    async function configureDatabase(config) {
        if (state.isDemoMode) {
            showToast('Cloud demo runs client-side. Launch run.bat locally for full Java/MySQL mode.', 'info');
            return { mode: 'DEMO' };
        }
        const res = await fetch('/api/db-config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Database configuration failed');
        }
        return data;
    }

    // ==========================================
    // Renderers
    // ==========================================

    function renderDbStatus(status) {
        const isEmbedded = status.mode === 'EMBEDDED' || (status.description && status.description.includes('Embedded'));
        elements.dbStatusDot.className = status.reachable ? 'status-pulse' : 'status-pulse pulse-warn';
        elements.dbStatusText.textContent = isEmbedded ? 'Embedded H2 Active' : 'MySQL Active';
        elements.footerDbName.textContent = status.description || 'Connected';
    }

    function renderStats(stats) {
        if (!stats) return;

        elements.statTotal.textContent = stats.total;
        elements.statAvgMarks.textContent = stats.averageMarks.toFixed(1) + '%';
        elements.statAvgGrade.textContent = stats.averageGrade;
        elements.statAvgGrade.className = `badge badge-grade grade-${formatGradeClass(stats.averageGrade)}`;

        if (stats.total > 0) {
            elements.statTopStudent.textContent = stats.topStudent;
            elements.statHighestMarks.textContent = `Score: ${stats.highestMarks.toFixed(1)}%`;
        } else {
            elements.statTopStudent.textContent = 'None';
            elements.statHighestMarks.textContent = 'Highest score: -';
        }

        // Render Grade distribution breakdown bars
        if (stats.gradeDistribution) {
            const grades = ['A+', 'A', 'B', 'C', 'D', 'F'];
            const maxCount = Math.max(...Object.values(stats.gradeDistribution), 1);

            elements.gradeDistContainer.innerHTML = grades.map(g => {
                const count = stats.gradeDistribution[g] || 0;
                const pct = stats.total > 0 ? (count / maxCount) * 100 : 0;
                const fillClass = `grade-${formatGradeClass(g)}`;
                return `
                    <div class="dist-item" title="${g}: ${count} student(s)">
                        <div class="dist-bar-track">
                            <div class="dist-bar-fill ${fillClass}" style="height: ${Math.max(pct, count > 0 ? 15 : 0)}%;"></div>
                        </div>
                        <span class="dist-label">${g}</span>
                        <span class="dist-count">${count}</span>
                    </div>
                `;
            }).join('');
        }
    }

    function renderTable() {
        const list = state.filteredStudents;

        if (list.length === 0) {
            elements.tableBody.innerHTML = '';
            elements.emptyState.classList.remove('hidden');

            const hasFilter = state.searchQuery || state.activeGradeFilter !== 'ALL';
            document.getElementById('empty-state-title').textContent = hasFilter
                ? 'No matching students found'
                : 'No Students Enrolled Yet';
            document.getElementById('empty-state-desc').textContent = hasFilter
                ? 'Try adjusting your search terms or grade filter to find student records.'
                : 'Get started by adding your first student record into the system.';
            return;
        }

        elements.emptyState.classList.add('hidden');

        elements.tableBody.innerHTML = list.map(student => {
            const initials = getInitials(student.name);
            const gradeClass = formatGradeClass(student.grade);
            const scorePercent = Math.min(Math.max(student.marks, 0), 100);

            return `
                <tr data-student-id="${student.id}">
                    <td class="student-id-cell">#${student.id}</td>
                    <td>
                        <div class="student-profile">
                            <div class="student-avatar" aria-hidden="true">${escapeHtml(initials)}</div>
                            <span class="student-name">${escapeHtml(student.name)}</span>
                        </div>
                    </td>
                    <td>
                        <a href="mailto:${escapeHtml(student.email)}" class="student-email-link" title="Send email to ${escapeHtml(student.name)}">
                            ${escapeHtml(student.email)}
                        </a>
                    </td>
                    <td>
                        <span class="course-badge">${escapeHtml(student.course)}</span>
                    </td>
                    <td>
                        <div class="score-cell">
                            <div class="score-info">
                                <span>${student.marks.toFixed(1)} / 100</span>
                                <span class="text-muted">${Math.round(scorePercent)}%</span>
                            </div>
                            <div class="score-track" title="${student.marks.toFixed(1)}% Marks">
                                <div class="score-fill grade-${gradeClass}" style="width: ${scorePercent}%;"></div>
                            </div>
                        </div>
                    </td>
                    <td style="text-align: center;">
                        <span class="badge badge-grade grade-${gradeClass}">${escapeHtml(student.grade)}</span>
                    </td>
                    <td style="text-align: right;">
                        <div class="action-buttons">
                            <button class="btn-icon btn-edit-student" data-id="${student.id}" title="Edit student details" aria-label="Edit student">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                </svg>
                            </button>
                            <button class="btn-icon btn-icon-danger btn-delete-student" data-id="${student.id}" data-name="${escapeHtml(student.name)}" title="Delete student record" aria-label="Delete student">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"></polyline>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                </svg>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    // ==========================================
    // Filter & Sort Engine
    // ==========================================

    function applyFilterAndSort() {
        let list = [...state.students];

        // Search Filter
        const query = state.searchQuery.toLowerCase().trim();
        if (query) {
            list = list.filter(s => 
                s.name.toLowerCase().includes(query) ||
                s.email.toLowerCase().includes(query) ||
                s.course.toLowerCase().includes(query) ||
                String(s.id).includes(query)
            );
        }

        // Grade Pill Filter
        if (state.activeGradeFilter !== 'ALL') {
            list = list.filter(s => s.grade === state.activeGradeFilter);
        }

        // Sorting
        list.sort((a, b) => {
            switch (state.currentSort) {
                case 'id-desc': return b.id - a.id;
                case 'name-asc': return a.name.localeCompare(b.name);
                case 'name-desc': return b.name.localeCompare(a.name);
                case 'marks-desc': return b.marks - a.marks;
                case 'marks-asc': return a.marks - b.marks;
                case 'id-asc':
                default:
                    return a.id - b.id;
            }
        });

        state.filteredStudents = list;
        renderTable();
    }

    // ==========================================
    // Grade Calculator Logic
    // ==========================================

    function calculateGrade(marks) {
        if (marks >= 90.0) return 'A+';
        if (marks >= 80.0) return 'A';
        if (marks >= 70.0) return 'B';
        if (marks >= 60.0) return 'C';
        if (marks >= 50.0) return 'D';
        return 'F';
    }

    function updateLiveGradePreview(marksVal) {
        const m = parseFloat(marksVal);
        const validMarks = isNaN(m) ? 0 : Math.min(Math.max(m, 0), 100);
        const grade = calculateGrade(validMarks);
        const gradeClass = formatGradeClass(grade);

        elements.previewGradeBadge.textContent = grade;
        elements.previewGradeBadge.className = `badge badge-grade grade-${gradeClass}`;
    }

    function formatGradeClass(grade) {
        if (!grade) return 'C';
        return grade === 'A+' ? 'A-plus' : grade;
    }

    function getInitials(name) {
        if (!name) return 'S';
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }

    // ==========================================
    // Modal Management
    // ==========================================

    function openAddModal() {
        state.editingStudentId = null;
        elements.modalStudentTitle.textContent = 'Add New Student';
        elements.btnSubmitText.textContent = 'Save Student';
        elements.formStudent.reset();
        elements.inputStudentId.value = '';
        elements.sliderStudentMarks.value = 85;
        elements.inputStudentMarks.value = 85;
        clearFormErrors();
        updateLiveGradePreview(85);
        elements.modalStudent.classList.remove('hidden');
        elements.inputStudentName.focus();
    }

    function openEditModal(student) {
        state.editingStudentId = student.id;
        elements.modalStudentTitle.textContent = `Edit Student #${student.id}`;
        elements.btnSubmitText.textContent = 'Update Student';
        clearFormErrors();

        elements.inputStudentId.value = student.id;
        elements.inputStudentName.value = student.name;
        elements.inputStudentEmail.value = student.email;
        elements.inputStudentCourse.value = student.course;
        elements.sliderStudentMarks.value = student.marks;
        elements.inputStudentMarks.value = student.marks;
        updateLiveGradePreview(student.marks);

        elements.modalStudent.classList.remove('hidden');
        elements.inputStudentName.focus();
    }

    function closeStudentModal() {
        elements.modalStudent.classList.add('hidden');
        clearFormErrors();
    }

    function openDeleteModal(id, name) {
        state.pendingDeleteId = id;
        elements.deleteStudentId.textContent = `#${id}`;
        elements.deleteStudentName.textContent = name;
        elements.modalDelete.classList.remove('hidden');
    }

    function closeDeleteModal() {
        elements.modalDelete.classList.add('hidden');
        state.pendingDeleteId = null;
    }

    function openDbModal() {
        elements.modalDbSettings.classList.remove('hidden');
    }

    function closeDbModal() {
        elements.modalDbSettings.classList.add('hidden');
    }

    function clearFormErrors() {
        ['error-name', 'error-email', 'error-course', 'error-marks'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.textContent = '';
        });
    }

    // ==========================================
    // Export to CSV
    // ==========================================

    function exportCsv() {
        const list = state.filteredStudents;
        if (!list || list.length === 0) {
            showToast('No students available to export.', 'info');
            return;
        }

        const headers = ['ID', 'Name', 'Email', 'Course', 'Marks', 'Grade'];
        const rows = list.map(s => [
            s.id,
            `"${s.name.replace(/"/g, '""')}"`,
            `"${s.email.replace(/"/g, '""')}"`,
            `"${s.course.replace(/"/g, '""')}"`,
            s.marks.toFixed(2),
            `"${s.grade}"`
        ]);

        const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        const dateStr = new Date().toISOString().slice(0, 10);
        link.setAttribute('href', url);
        link.setAttribute('download', `students_export_${dateStr}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        showToast('Student list exported to CSV successfully.', 'success');
    }

    // ==========================================
    // Toast Notification System
    // ==========================================

    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const iconSvg = type === 'success' 
            ? '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#10b981" stroke-width="2"><path d="M20 6L9 17l-5-5"/></svg>'
            : type === 'error'
            ? '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#ef4444" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
            : '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#6366f1" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';

        toast.innerHTML = `
            ${iconSvg}
            <span>${escapeHtml(message)}</span>
        `;

        elements.toastContainer.appendChild(toast);
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 4000);
    }

    // ==========================================
    // Event Listeners
    // ==========================================

    function bindEvents() {
        // Theme toggle
        elements.themeToggle.addEventListener('click', toggleTheme);

        // Search Input
        elements.inputSearch.addEventListener('input', e => {
            state.searchQuery = e.target.value;
            elements.btnClearSearch.classList.toggle('hidden', !state.searchQuery);
            applyFilterAndSort();
        });

        elements.btnClearSearch.addEventListener('click', () => {
            elements.inputSearch.value = '';
            state.searchQuery = '';
            elements.btnClearSearch.classList.add('hidden');
            applyFilterAndSort();
        });

        // Grade Filter Pills
        elements.filterPills.addEventListener('click', e => {
            const btn = e.target.closest('.pill-btn');
            if (!btn) return;
            elements.filterPills.querySelectorAll('.pill-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeGradeFilter = btn.dataset.grade;
            applyFilterAndSort();
        });

        // Sort Dropdown
        elements.selectSort.addEventListener('change', e => {
            state.currentSort = e.target.value;
            applyFilterAndSort();
        });

        // Add Student Buttons
        elements.btnOpenAddModal.addEventListener('click', openAddModal);
        elements.btnEmptyAdd.addEventListener('click', openAddModal);
        elements.btnCloseStudentModal.addEventListener('click', closeStudentModal);
        elements.btnCancelStudentModal.addEventListener('click', closeStudentModal);

        // Synchronize Marks Slider & Number Input
        elements.sliderStudentMarks.addEventListener('input', e => {
            elements.inputStudentMarks.value = e.target.value;
            updateLiveGradePreview(e.target.value);
        });

        elements.inputStudentMarks.addEventListener('input', e => {
            elements.sliderStudentMarks.value = e.target.value || 0;
            updateLiveGradePreview(e.target.value);
        });

        // Form Submit: Add or Edit Student
        elements.formStudent.addEventListener('submit', async e => {
            e.preventDefault();
            clearFormErrors();

            const name = elements.inputStudentName.value.trim();
            const email = elements.inputStudentEmail.value.trim();
            const course = elements.inputStudentCourse.value.trim();
            const marks = parseFloat(elements.inputStudentMarks.value);

            let hasError = false;
            if (!name) {
                document.getElementById('error-name').textContent = 'Please enter a student name.';
                hasError = true;
            }
            if (!email || !email.includes('@')) {
                document.getElementById('error-email').textContent = 'Please enter a valid email address.';
                hasError = true;
            }
            if (!course) {
                document.getElementById('error-course').textContent = 'Please specify a course.';
                hasError = true;
            }
            if (isNaN(marks) || marks < 0 || marks > 100) {
                document.getElementById('error-marks').textContent = 'Marks must be between 0 and 100.';
                hasError = true;
            }

            if (hasError) return;

            try {
                const result = await saveStudent({ name, email, course, marks });
                closeStudentModal();
                showToast(state.editingStudentId ? 'Student record updated.' : 'Student enrolled successfully!', 'success');
                refreshAll();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });

        // Table Action Buttons (Delegated)
        elements.tableBody.addEventListener('click', e => {
            const editBtn = e.target.closest('.btn-edit-student');
            if (editBtn) {
                const id = parseInt(editBtn.dataset.id);
                const student = state.students.find(s => s.id === id);
                if (student) openEditModal(student);
                return;
            }

            const deleteBtn = e.target.closest('.btn-delete-student');
            if (deleteBtn) {
                const id = parseInt(deleteBtn.dataset.id);
                const name = deleteBtn.dataset.name;
                openDeleteModal(id, name);
                return;
            }
        });

        // Delete Confirm / Cancel
        elements.btnCancelDelete.addEventListener('click', closeDeleteModal);
        elements.btnConfirmDelete.addEventListener('click', async () => {
            if (!state.pendingDeleteId) return;
            try {
                await deleteStudent(state.pendingDeleteId);
                closeDeleteModal();
                showToast('Student record deleted successfully.', 'success');
                refreshAll();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });

        // Export CSV
        elements.btnExportCsv.addEventListener('click', exportCsv);

        // Database Modal
        elements.dbStatusBtn.addEventListener('click', openDbModal);
        elements.footerDbLink.addEventListener('click', e => {
            e.preventDefault();
            openDbModal();
        });
        elements.btnCloseDbModal.addEventListener('click', closeDbModal);

        // Database Tabs
        elements.tabDbEmbedded.addEventListener('click', () => {
            elements.tabDbEmbedded.classList.add('active');
            elements.tabDbMysql.classList.remove('active');
            elements.tabContentEmbedded.classList.remove('hidden');
            elements.tabContentMysql.classList.add('hidden');
        });

        elements.tabDbMysql.addEventListener('click', () => {
            elements.tabDbMysql.classList.add('active');
            elements.tabDbEmbedded.classList.remove('active');
            elements.tabContentMysql.classList.remove('hidden');
            elements.tabContentEmbedded.classList.add('hidden');
        });

        // Switch to Embedded Mode
        elements.btnApplyEmbedded.addEventListener('click', async () => {
            try {
                await configureDatabase({ mode: 'EMBEDDED' });
                showToast('Switched to Embedded Database (H2).', 'success');
                closeDbModal();
                refreshAll();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });

        // Connect to MySQL
        elements.formMysqlConfig.addEventListener('submit', async e => {
            e.preventDefault();
            const config = {
                mode: 'MYSQL',
                host: elements.inputMysqlHost.value.trim(),
                port: elements.inputMysqlPort.value.trim(),
                database: elements.inputMysqlDb.value.trim(),
                user: elements.inputMysqlUser.value.trim(),
                password: elements.inputMysqlPass.value
            };

            try {
                await configureDatabase(config);
                showToast('Connected to MySQL successfully!', 'success');
                closeDbModal();
                refreshAll();
            } catch (err) {
                showToast('MySQL Error: ' + err.message, 'error');
            }
        });

        // Close modals on clicking backdrop or ESC key
        [elements.modalStudent, elements.modalDelete, elements.modalDbSettings].forEach(modal => {
            modal.addEventListener('click', e => {
                if (e.target === modal) modal.classList.add('hidden');
            });
        });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                closeStudentModal();
                closeDeleteModal();
                closeDbModal();
            }
        });
    }

    // Launch!
    init();
});
