import React, { useState } from 'react';

import { IoCloseOutline } from "react-icons/io5";

export interface BookingSelection {
  roomId: number;
  checkIn: string;
  checkOut: string;
  guests: number;
  total: number;
}

interface ModalProps {
  isOpen: boolean;
  closeModal: () => void;
  booking: BookingSelection;
}

export default function ConfirmBookingModal({ closeModal, isOpen, booking }: ModalProps) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    // Handle form submission logic (e.g., send data to an API)
    console.log('Booking Details:', { name, phone, ...booking });
    closeModal(); // Close the modal after submission
  };

  if (!isOpen) return null; // Render nothing if modal is closed

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
      <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-6 relative flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 pb-4 mb-4">
          <h2 className="text-xl font-semibold text-gray-800">Confirm Booking</h2>
          <button
            onClick={closeModal}
            className="text-gray-600 hover:text-gray-900"
            aria-label="Close Modal"
          >
            <IoCloseOutline className="h-6 w-6" />
          </button>
        </div>

        {/* Room Image */}
        <img
          src="https://via.placeholder.com/150"
          alt="Room Preview"
          className="w-full h-48 object-cover rounded-md mb-4"
        />

        {/* Booking Details */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-gray-600 font-semibold">Name</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full border border-gray-300 rounded-md p-2"
              placeholder="Enter your name"
              required
            />
          </div>
          <div>
            <label htmlFor="phone" className="block text-gray-600 font-semibold">Phone</label>
            <input
              id="phone"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full border border-gray-300 rounded-md p-2"
              placeholder="Enter your phone number"
              required
            />
          </div>
          <dl className="grid grid-cols-2 gap-2 text-gray-600">
            <dt className="font-semibold">Check-in</dt>
            <dd>{booking.checkIn}</dd>
            <dt className="font-semibold">Check-out</dt>
            <dd>{booking.checkOut}</dd>
            <dt className="font-semibold">Guests</dt>
            <dd>{booking.guests}</dd>
            <dt className="font-semibold">Total</dt>
            <dd>${booking.total}</dd>
          </dl>
        </form>

        {/* Divider */}
        <hr className="my-6 border-gray-200" />

        {/* Footer */}
        <div className="flex justify-end space-x-4 mt-4">
          <button
            onClick={closeModal}
            className="bg-gray-200 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-300"
          >
            Cancel
          </button>
          <button
            type="submit"
            onClick={handleSubmit}
            className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
          >
            Confirm Booking
          </button>
        </div>
      </div>
    </div>
  );
}
